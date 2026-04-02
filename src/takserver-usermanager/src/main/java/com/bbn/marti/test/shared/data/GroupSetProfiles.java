package com.bbn.marti.test.shared.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created on 10/13/15.
 */
public enum GroupSetProfiles {
	Set_None(),
	Set_0(GroupProfiles.group0),
	Set_01(GroupProfiles.group0, GroupProfiles.group1),
	Set_02(GroupProfiles.group0, GroupProfiles.group2),
	Set_03(GroupProfiles.group0, GroupProfiles.group3),
	Set_012(GroupProfiles.group0, GroupProfiles.group1, GroupProfiles.group2),
	Set_013(GroupProfiles.group0, GroupProfiles.group1, GroupProfiles.group2),
	Set_023(GroupProfiles.group0, GroupProfiles.group2, GroupProfiles.group3),
	Set_0123(GroupProfiles.group0, GroupProfiles.group1, GroupProfiles.group2, GroupProfiles.group3),
	Set_1(GroupProfiles.group1),
	Set_12(GroupProfiles.group1, GroupProfiles.group2),
	Set_13(GroupProfiles.group1, GroupProfiles.group3),
	Set_123(GroupProfiles.group1, GroupProfiles.group2, GroupProfiles.group3),
	Set_2(GroupProfiles.group2),
	Set_23(GroupProfiles.group2, GroupProfiles.group3),
	Set_3(GroupProfiles.group3),
	Set_Anon(GroupProfiles.__ANON__);

	@NotNull
	private final String tag;


	@NotNull
	public String displayString() {
		if (groupsSet.isEmpty()) {
			return "[]";
		}

		String returnString = null;

		for (GroupProfiles group : groupsSet) {
			if (returnString == null) {
				returnString = "[" + group.name();
			} else {
				returnString += ", " + group.name();
			}
		}
		returnString += "]";
		return returnString;
	}

	@NotNull
	public final String getTag() {
		return tag;
	}

	@NotNull
	private final Set<GroupProfiles> groupsSet;

	public final TreeSet<String> groupSet;

	public final String[] stringArray() {
		return groupSet.toArray(new String[0]);
	}

	GroupSetProfiles(@Nullable GroupProfiles... groupIdentifiers) {
		String groupTag = "";

		Set<GroupProfiles> set = new HashSet<>();
		TreeSet<String> stringSet = new TreeSet<>();

		if (groupIdentifiers != null) {
			for (GroupProfiles group : groupIdentifiers) {
				set.add(group);
				stringSet.add(group.name());
				groupTag = groupTag + group.getIdentifier();
			}
		}

		groupsSet = set;
		groupSet = stringSet;
		tag = groupTag;
	}

	public boolean intersects(@NotNull GroupSetProfiles groupSetToCheck) {
		if (this == GroupSetProfiles.Set_None || groupSetToCheck == GroupSetProfiles.Set_None) {
			return false;
		}

		return intersects(groupSetToCheck.groupsSet);
	}

	public boolean intersects(@NotNull Set<GroupProfiles> compareGroupSet) {
		for (GroupProfiles group : groupsSet) {
			if (compareGroupSet.contains(group)) {
				return true;
			}
		}
		return false;
	}

	public TreeSet<String> getIntersectingGroupNames(@NotNull GroupSetProfiles groupSetToCheck) {
		TreeSet<String> intersectingNames = new TreeSet<>();

		Set<GroupProfiles> otherGroupSet = groupSetToCheck.groupsSet;

		for (GroupProfiles group : groupsSet) {
			if (otherGroupSet.contains(group)) {
				intersectingNames.add(group.name());
			}
		}
		return intersectingNames;
	}

	public TreeSet<String> getIntersectingGroupNames(@NotNull TreeSet<String> groupSet) {
		TreeSet<String> result = new TreeSet<>(groupSet);
		result.retainAll(this.groupSet);
		return result;
	}

	public Set<GroupProfiles> getGroups() {
		return groupsSet;
	}
}
