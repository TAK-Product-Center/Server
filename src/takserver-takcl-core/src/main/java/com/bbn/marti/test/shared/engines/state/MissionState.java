package com.bbn.marti.test.shared.engines.state;

import com.bbn.marti.test.shared.data.GroupSetProfiles;
import com.bbn.marti.test.shared.data.users.AbstractUser;
import com.bbn.marti.test.shared.engines.ActionEngine;
import com.bbn.marti.tests.Assert;
import org.dom4j.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

import static com.bbn.marti.takcl.connectivity.missions.MissionModels.*;

public class MissionState implements Comparable<MissionState> {
	private final AbstractUser missionOwner;
	private final String missionName;
	private GroupSetProfiles groupSetProfile;
	private final TreeMap<AbstractUser, SubscriptionData> subscriptionDataMap = new TreeMap<>();
	private final MissionUserRole defaultRole;
	private final TreeMap<AbstractUser, MissionUserRole> userRoles = new TreeMap<>();
	private final LinkedList<MissionChange> missionChanges = new LinkedList<>();
	private String password = null;
	private boolean deleted = false;

	private final boolean defaultRoleInUse;
	private Mission mission;

	public MissionState(@NotNull AbstractUser missionOwner, @NotNull String missionName,
	                    @NotNull GroupSetProfiles groupSetProfile, @NotNull Mission mission,
	                    @Nullable MissionUserRole defaultRole, @Nullable String password) {
		this.missionOwner = missionOwner;
		this.missionName = missionName;
		this.groupSetProfile = groupSetProfile;
		this.mission = mission;
		this.password = password;

		if (defaultRole == null) {
			this.defaultRole = DEFAULT_MISSION_USER_ROLE;
			this.defaultRoleInUse = true;
		} else {
			this.defaultRole = defaultRole;
			this.defaultRoleInUse = false;
		}

		MissionChange addMissionChange = new MissionChange();
		addMissionChange.creatorUid = mission.getCreatorUid();
		addMissionChange.missionName = mission.getUniqueStableName();
		addMissionChange.serverTime = mission.getCreateTime();
		addMissionChange.timestamp = mission.getCreateTime();
		addMissionChange.type = MissionChangeType.CREATE_MISSION;
		missionChanges.add(addMissionChange);
	}

	synchronized void setGroupSetProfile(GroupSetProfiles newProfile) {
		if (this.groupSetProfile != newProfile) {
			this.groupSetProfile = newProfile;
		}
	}

	public synchronized MissionUserRole getUserRole(@NotNull AbstractUser user) {
		if (subscriptionDataMap.containsKey(user)) {
			return subscriptionDataMap.get(user).getRole();
		} else {
			return defaultRole;
		}
	}

	void setDeleted() {
		deleted = true;
	}

	public boolean hasBeenDeleted() {
		return deleted;
	}

	public boolean isDefaultRoleInUse() {
		return defaultRoleInUse;
	}

	public synchronized void addSentCotMessage(@NotNull Document sentCotMesage) {
		MissionContentDataContainer mcdc = MissionContentDataContainer.fromSentCotDocument(sentCotMesage);
		mission.addUidData(mcdc);

		MissionChange missionChange = MissionChange.fromSentCotDocument(sentCotMesage);
		innerAddMissionChange(missionChange);
	}

	public synchronized void addMissionResource(@NotNull MissionContentDataContainer dataContainer) {
		innerAddResource(dataContainer);
		MissionChange missionChange = dataContainer.toMissionChangeAddition(missionName);
		innerAddMissionChange(missionChange);
	}

	public synchronized void removeMissionResource(@NotNull AbstractUser removingUser, @NotNull String dataUploadHash) {
		mission.getContents().removeIf(x -> x.getDataAsMissionContent().getContentHash().equals(dataUploadHash));

		List<MissionChange> missionChangeList = missionChanges;
		MissionChange missionChange = missionChangeList.stream().filter(x -> x.contentResource != null && dataUploadHash.equals(x.contentResource.getContentHash())).findFirst().get();
		missionChangeList.add(0, missionChange.obsoleteContentResourceAndProduceDeletionStatement(removingUser.getCotUid()));
	}

	private  synchronized void innerAddResource(@NotNull MissionContentDataContainer dataContainer) {
		mission.getContents().add(dataContainer);
	}

	private synchronized void innerAddMissionChange(@NotNull MissionChange missionChange) {
		if (missionChange.contentUid != null && !missionChange.contentUid.equals("")) {
			Optional<MissionChange> candidate = missionChanges.stream().filter(x -> missionChange.contentUid.equals(x.contentUid)).findFirst();
			candidate.ifPresent(missionChanges::remove);
		}
		missionChanges.addFirst(missionChange);
	}


	public boolean assertMissionMatchesExpectations(@NotNull Mission actualMission, @Nullable Map<Pattern, Object> exceptions) {
		return actualMission.assertMatchesExpectation(mission, exceptions);

	}

	public boolean assertMissionChangesMatchExpectations(@NotNull List<MissionChange> actualMissionChanges, @Nullable Map<Pattern, Object> exceptions) {
		Assert.assertEquals("The number of received mission changes does not match the number of expected mission changes!",
				missionChanges.size(), actualMissionChanges.size());

		boolean passed = true;
		for (int i = 0; i < actualMissionChanges.size(); i++) {
			passed = passed && actualMissionChanges.get(i).assertMatchesExpectation(missionChanges.get(i), exceptions);
		}
		return passed;
	}

	public synchronized void addUserSubscription(@NotNull AbstractUser user, @NotNull SubscriptionData data) {
		subscriptionDataMap.put(user, data);
	}

	synchronized void updateUserSubscriptionRole(@NotNull AbstractUser user, MissionUserRole role) {
		subscriptionDataMap.put(user, subscriptionDataMap.get(user).cloneWithNewRole(role));
	}

	public synchronized SubscriptionData cloneUserSubscriptionWithNewRole(@NotNull AbstractUser user, @NotNull MissionUserRole newRole) {
		return subscriptionDataMap.get(user).cloneWithNewRole(newRole);

	}

	public synchronized boolean isSubscriber(@NotNull AbstractUser user) {
		return subscriptionDataMap.keySet().contains(user);
	}

	public synchronized String getSubscriberToken(@NotNull AbstractUser user) {
		if (subscriptionDataMap.containsKey(user)) {
			return subscriptionDataMap.get(user).getToken();
		} else {
			return null;
		}
	}

	public synchronized GroupSetProfiles getGroupSet() {
		return groupSetProfile;
	}

	public final Mission getMission() {
		return mission;
	}

	public void setMissionKeywords(String... keywords) {
		mission.setKeywords(keywords);
	}

	public void clearMissionKeywords() {
		mission.clearKeywords();
	}

	public final String getMissionName() {
		return missionName;
	}

	public synchronized void setPassword(@NotNull String password) {
		mission.setPasswordProtected(true);
		this.password = password;
	}

	@Nullable
	public synchronized String getPassword() {
		return password;
	}

	@Override
	public int compareTo(@NotNull MissionState o) {
		return missionName.compareTo(o.missionName);
	}
}
