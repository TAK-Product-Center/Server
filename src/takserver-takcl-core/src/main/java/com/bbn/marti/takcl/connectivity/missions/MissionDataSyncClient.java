package com.bbn.marti.takcl.connectivity.missions;

import com.bbn.marti.takcl.SSLHelper;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


import static com.bbn.marti.takcl.connectivity.missions.MissionModels.ApiListResponse;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.ApiSetResponse;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.ApiSingleResponse;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.EnterpriseSyncUploadResponse;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.Mission;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.MissionChange;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.MissionUserRole;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.PutMissionContents;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.ResponseWrapper;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.SubscriptionData;
import static com.bbn.marti.takcl.connectivity.missions.MissionModels.gson;

public class MissionDataSyncClient {

	private final AbstractUser user;
	private final String baseUrl;

	private Retrofit _retrofit;
	private MissionDataSyncInterface _missionApi;
	private EnterpriseSyncInterface _fileApi;

	private final Logger log = LoggerFactory.getLogger(MissionDataSyncClient.class);

	public MissionDataSyncClient(@NotNull AbstractUser user) {
		this.user = user;
		this.baseUrl = user.getServer().getMissionBaseUrl();
	}

	protected synchronized <T> T createApi(Class<T> interfaceClass) {
		try {
			if (_retrofit == null) {
				SSLHelper.TakClientSslContext tcsc = new SSLHelper.TakClientSslContext(user);

				CustomHttpLoggingInterceptor loggingInterceptor = new CustomHttpLoggingInterceptor(log::info);
				loggingInterceptor.setLevel(CustomHttpLoggingInterceptor.Level.BODY);

				tcsc.init();
				OkHttpClient.Builder builder = new OkHttpClient.Builder()
						.sslSocketFactory(tcsc.getSslSocketFactory(), tcsc.getTrustManager())
						.addInterceptor(loggingInterceptor)
						.callTimeout(100, TimeUnit.SECONDS)
						.connectTimeout(100, TimeUnit.SECONDS)
						.readTimeout(100, TimeUnit.SECONDS)
						.writeTimeout(100, TimeUnit.SECONDS)
						.hostnameVerifier((s, sslSession) -> {
							// TODO: Not this
							return true;
						});

				_retrofit = new Retrofit.Builder()
						.baseUrl(baseUrl)
						.addConverterFactory(ScalarsConverterFactory.create())
						.addConverterFactory(GsonConverterFactory.create(gson))
						.client(builder.build())
						.build();
			}
			return _retrofit.create(interfaceClass);
		} catch (GeneralSecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private synchronized EnterpriseSyncInterface fileApi() {
		if (_fileApi == null) {
			_fileApi = createApi(EnterpriseSyncInterface.class);
		}
		return _fileApi;
	}

	private synchronized MissionDataSyncInterface missionApi() {
		if (_missionApi == null) {
			_missionApi = createApi(MissionDataSyncInterface.class);
		}
		return _missionApi;
	}

	private <T> ResponseWrapper<T> getResponse(Call<T> call) {
		try {
			Response<T> response = call.execute();
			T result = response.body();
			int code = response.code();
			String errorBody = null;
			if (response.errorBody() != null) {
				errorBody = response.errorBody().string();
			}
			return new ResponseWrapper<>(code, result, errorBody);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String getResponseString(Call<String> call) {
		try {
			Response<String> response = call.execute();
			if (response.isSuccessful()) {
				return response.body();
			} else {
				if (response.code() == 403) {
					return null;
				} else {
					throw new RuntimeException("Failed!");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized ResponseWrapper<ApiSetResponse<Mission>> getAllMissions() {
		log.info(user.toString() + " fetching missions");
		return getResponse(missionApi().getAllMissions());
	}

	public synchronized ResponseWrapper<ApiSetResponse<Mission>> getMissionByName(@NotNull String missionName,
	                                                                              @Nullable String password) {
		log.info(user.toString() + " fetching mission \"" + missionName + "\"");
		return getResponse(missionApi().getMission(missionName, password));
	}

	public synchronized String getMissionCot(@NotNull String mission) {
		log.info(user.toString() + " fetching mission \"" + mission + "\" CoT");
		return getResponseString(missionApi().getMissionCot(mission));
	}

	public synchronized ResponseWrapper<ApiSetResponse<Mission>> addMission(@NotNull String missionName, @Nullable MissionUserRole role,
	                                                                        @Nullable String tool, @Nullable String... group) {
		log.info(user.toString() + " adding Mission \"" + missionName + "\"");
		return getResponse(missionApi().createMission(missionName, role, null, tool, group));
	}

	public synchronized ResponseWrapper<ApiSetResponse<Mission>> deleteMission(@NotNull String missionName) {
		log.info(user.toString() + " deleting mission \"" + missionName + "\"");
		return getResponse(missionApi().deleteMission(missionName, false, null));
	}

	public synchronized ResponseWrapper<ApiSetResponse<Mission>> deepDeleteMission(@NotNull String missionName, @NotNull String subscriptionToken) {
		log.info(user.toString() + " deleting mission \"" + missionName + "\"");
		return getResponse(missionApi().deleteMission(missionName, true, "Bearer " + subscriptionToken));
	}

	public synchronized ResponseWrapper<ApiSetResponse<Mission>> addMissionContents(@NotNull String mission, @NotNull PutMissionContents contents, @Nullable String creatorUid) {
		log.info(user.toString() + " adding contents to mission \"" + mission + "\"");
		return getResponse(missionApi().addMissionContent(mission, contents, creatorUid));
	}

	public synchronized ResponseWrapper<ApiSetResponse<Mission>> deleteMissionHash(@NotNull String mission, @NotNull String hash, @Nullable String creatorUid, @Nullable String subscriptionToken) {
		log.info(user.toString() + " deleting content hash \"" + hash + "\" from mission \"" + mission + "\"");
		return getResponse(missionApi().removeMissionContent(mission, hash, creatorUid,
				subscriptionToken == null ? null : "Bearer " + subscriptionToken));
	}

	public synchronized ResponseWrapper<ApiSingleResponse<SubscriptionData>> createMissionSubscription(@NotNull String missionName, @NotNull String clientUid) {
		return getResponse(missionApi().createMissionSubscription(missionName, clientUid));
	}

	public synchronized ResponseWrapper<Void> setMissionSubscriptionRole(@NotNull String missionName, @NotNull SubscriptionData subscriptionData, @Nullable String subscriptionToken) {
		return getResponse(missionApi().setMissionRole(missionName, subscriptionData.getClientUid(), subscriptionData.role.type,
				subscriptionToken == null ? null : "Bearer " + subscriptionToken));
	}

	public synchronized ResponseWrapper<ApiSetResponse<SubscriptionData>> getMissionSubscriptions(@NotNull String missionName) {
		return getResponse(missionApi().getMissionSubscriptionRoles(missionName));
	}

	public synchronized ResponseWrapper<ApiListResponse<MissionChange>> getMissionChanges(@NotNull String missionName) {
		return getResponse(missionApi().getMissionChanges(missionName, null, null, null));
	}

	public synchronized ResponseWrapper<ApiSetResponse<Mission>> setMissionKeywords(@NotNull String missionName, @Nullable String subscriptionToken, String... keywords) {
		return getResponse(missionApi().setKeywords(missionName, user.getCotUid(), Arrays.asList(keywords),
				subscriptionToken == null ? null : "Bearer " + subscriptionToken));
	}

	public synchronized ResponseWrapper<Void> clearMissionKeywords(@NotNull String missionName, @Nullable String subscriptionToken) {
		return getResponse(missionApi().clearKeywords(missionName,
				subscriptionToken == null ? null : "Bearer " + subscriptionToken));
	}

	public synchronized ResponseWrapper<Void> setMissionPassword(@NotNull String missionName, @NotNull String password,
	                                                             @Nullable String creatorUid, @Nullable String subscriptionToken) {
		return getResponse(missionApi().setPassword(missionName, password, creatorUid,
				subscriptionToken == null ? null : "Bearer " + subscriptionToken));
	}

	public synchronized ResponseWrapper<String> fileDelete(@NotNull String hash) {
		return getResponse(fileApi().fileDelete(hash));
	}

	public synchronized ResponseWrapper<byte[]> fileDownload(@NotNull String hash) {
		return getResponse(fileApi().fileDownload(hash));
	}

	public synchronized ResponseWrapper<EnterpriseSyncUploadResponse> fileUpload(@NotNull String name, @NotNull byte[] data) {
		String extension = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
		int contentLength = data.length;
		String contentType;
		switch (extension) {
			case "png":
				contentType = "image/png";
				break;
			case "jpg":
			case "jpeg":
				contentType = "image/jpeg";
				break;
			case "txt":
				contentType = "text/plain";
				break;
			case "json":
				contentType = "application/json";
				break;
			default:
				contentType = "application/binary";
		}

		return getResponse(fileApi().fileUpload(name, contentType, contentLength, data));
	}
}
