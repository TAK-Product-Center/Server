package com.bbn.marti.takcl.connectivity.missions;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Date;
import java.util.List;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.*;

public interface MissionDataSyncInterface {

	/*
	 * Skipping:
	 * Mission Contents Search
	 * Get Mission Subscriptions
	 * Mission Invitations
	   - How to accept?
	 * Mission Packages
	 * Mission Hierarchy
	 * External Data
	   - What is this?
	 * Mission KML
	 */

	@GET("Marti/api/missions")
	@Headers({"Content-Type: application/json;charset=UTF-8"})
	Call<ApiSetResponse<Mission>> getAllMissions();

	@GET("Marti/api/missions/{mission}")
	@Headers({"Content-Type: application/json;charset=UTF-8"})
	Call<ApiSetResponse<Mission>> getMission(@NotNull @Path("mission") String mission,
	                                         @Nullable @Query("password") String password);

	@GET("Marti/api/missions/{mission}/cot")
	Call<String> getMissionCot(@NotNull @Path("mission") String mission);

	@PUT("Marti/api/missions/{name}")
	Call<ApiSetResponse<Mission>> createMission(@NotNull @Path("name") String name,
	                                            @Nullable @Query("defaultRole") MissionUserRole role,
	                                            @Nullable @Query("creatorId") String creatorUid,
	                                            @Nullable @Query("tool") String tool,
	                                            @Nullable @Query("group") String... group);

	@DELETE("Marti/api/missions/{mission}")
	Call<ApiSetResponse<Mission>> deleteMission(@NotNull @Path("mission") String mission,
	                                            @NotNull
	                                            @Nullable @Query("deepDelete") boolean deepDelete,
	                                            @Nullable @Header("Authorization") String subscriptionToken);

	// TODO: The return type does not match, hashes appear to be ignored.
	@PUT("Marti/api/missions/{mission}/contents")
	@Headers({"Content-Type: application/json"})
	Call<ApiSetResponse<Mission>> addMissionContent(@NotNull @Path("mission") String mission,
	                                                @NotNull @Body PutMissionContents contents,
	                                                @Nullable @Query("creatorUid") String creatorUid);

	@DELETE("Marti/api/missions/{mission}/contents")
	Call<ApiSetResponse<Mission>> removeMissionContent(@NotNull @Path("mission") String mission,
	                                                   @NotNull @Query("hash") String hash,
	                                                   @Nullable @Query("creatorUid") String creatorUid,
	                                                   @Nullable @Header("Authorization") String subscriptionToken);


	@DELETE("Marti/api/missions/{mission}/contents")
	Call<ApiSetResponse<Mission>> deleteMissionContentsByUid(@NotNull @Path("mission") String mission,
	                                                         @NotNull @Query("uid") String uid,
	                                                         @Nullable @Query("creatorUid") String creatorUid);

	@GET("Marti/api/missions/{mission}/changes")
	Call<ApiListResponse<MissionChange>> getMissionChanges(@NotNull @Path("mission") String mission,
	                                                @Nullable @Query("secago") Integer secondsAgo,
	                                                @Nullable @Query("start") Date start,
	                                                @Nullable @Query("end") Date end);


	@PUT("Marti/api/missions/{mission}/keywords")
	@Headers({"Content-Type: application/json"})
	Call<ApiSetResponse<JsonObject>> addMissionKeywords(@NotNull @Path("mission") String mission,
	                                                 @NotNull @Body String... keywords);

	@DELETE("Marti/api/missions/{mission}/keywords")
	Call<ApiSetResponse<JsonObject>> deleteMissionKeywords(@NotNull @Path("mission") String mission);

	@DELETE("Marti/api/missions/{mission}/keywords/{keyword}")
	Call<ApiSetResponse<JsonObject>> deleteMissionKeyword(@NotNull @Path("mission") String mission,
	                                                   @NotNull @Path("keyword") String keyword);

	@POST("Marti/api/missions/logs/entries")
	@Headers({"Content-Type: application/json;charset=UTF-8"})
	Call<ApiSetResponse<JsonObject>> addLogEntry(@NotNull @Body LogEntry logEntry);

	@GET("Marti/api/missions/{mission}/log")
	Call<ApiSetResponse<JsonObject>> getMissionLog(@NotNull @Path("mission") String mission,
	                                            @Nullable @Query("secago") Integer secondsAgo,
	                                            @Nullable @Query("start") Date start,
	                                            @Nullable @Query("end") Date end);

	@DELETE("Marti/api/missions/logs/entries/{entry_id}")
	Call<ApiSetResponse<JsonObject>> deleteLogEntry(@NotNull @Path("entry_id") String entryId);

	@PUT("Marti/api/missions/logs/entries")
	@Headers({"Content-Type: application/json;charset=UTF-8"})
	Call<ApiSetResponse<JsonObject>> updateLogEntry(@NotNull @Path("entry_id") String entryId);

	// Skipping topic parameter used with WebCOP
	@PUT("Marti/api/missions/{mission}/subscription")
	Call<Void> missionSubscribe(@NotNull @Path("mission") String mission, @NotNull @Query("uid") String clientUid);

	// Skipping topic parameter used with WebCOP
	@DELETE("Marti/api/missions/{mission}/subscription")
	Call<String> missionUnsubscribe(@NotNull @Path("mission") String mission, @NotNull @Query("uid") String uid);

	@PUT("Marti/api/missions/{mission}/subscription")
	Call<ApiSingleResponse<SubscriptionData>> createMissionSubscription(@NotNull @Path("mission") String missionName, @NotNull @Query("uid") String uid);

	@PUT("Marti/api/missions/{mission}/keywords")
	Call<ApiSetResponse<Mission>> setKeywords(@NotNull @Path("mission") String mission,
	                                          @Nullable @Query("creatorUid") String creatorUid,
	                                          @NotNull @Body List<String> keywords,
	                                          @Nullable @Header("Authorization") String subscriptionToken);

	@DELETE("Marti/api/missions/{mission}/keywords")
	Call<Void> clearKeywords(@NotNull @Path("mission") String missionName,
	                         @Nullable @Header("Authorization") String subscriptionToken);

	@POST("Marti/api/missions/{mission}/subscription")
	Call<Void> setSubscriptionRole(@NotNull @Path("mission") String missionName,
	                               @Nullable @Query("creatorUid") String creatorUid,
	                               @NotNull @Body List<SubscriptionData> subscriptions);

	@GET("Marti/api/missions/{mission}/subscriptions/roles")
	Call<ApiSetResponse<SubscriptionData>> getMissionSubscriptionRoles(@NotNull @Path("mission") String missionName);

	@PUT("Marti/api/missions/{mission}/password")
	Call<Void> setPassword(@NotNull @Path("mission") String missionName, @NotNull @Query("password") String password,
	                       @Nullable @Query("creatorUid") String creatorUid,
	                       @Nullable @Header("Authorization") String subscriptionToken);

	@PUT("Marti/api/missions/{mission}/role")
	Call<Void> setMissionRole(@NotNull @Path("mission") String missionName,
	                          @NotNull @Query("clientUid") String clientUid,
	                          @NotNull @Query("role") MissionUserRole role,
	                          @Nullable @Header("Authorization") String subscriptionToken);
}
