package com.bbn.marti.takcl.connectivity.missions;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import com.bbn.marti.takcl.connectivity.missions.MissionModels.EnterpriseSyncUploadResponse;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface EnterpriseSyncInterface {
	@POST("Marti/sync/upload")
	Call<EnterpriseSyncUploadResponse> fileUpload(@NotNull @Query("name") String filename,
	                                              @NotNull @Header("Content-Type") String contentType,
	                                              @NotNull @Header("Content-Length") int contentLengthBytes,
	                                              @NotNull @Body byte[] data);

//	@POST("Marti/sync/upload")
//	Call<JsonObject> fileUpload(@NotNull @Query("name") String filename,
//	                            @NotNull @Header("Content-Type") String contentType,
//	                            @NotNull @Header("Content-Length") int contentLengthBytes,
//	                            @NotNull @Body byte[] data,
//	                            @Nullable @Query("uid") String uid,
//	                            @Nullable @Query("latitude") double latitude,
//	                            @Nullable @Query("longitude") double longitude,
//	                            @Nullable @Query("creatorUid") String creatorUid,
//	                            @Nullable @Query("keywords") String... keywords);


	@GET("Marti/sync/content")
	Call<byte[]> fileDownload(@NotNull @Query("hash") String hash);

	@POST("Marti/sync/delete")
	Call<String> fileDelete(@NotNull @Query("hash") String hash);
}
