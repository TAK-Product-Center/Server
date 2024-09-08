

package com.bbn.marti.remote;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.atakmap.Tak.ROL;
import com.bbn.marti.config.Federation;
import com.bbn.marti.config.Federation.Federate;
import com.bbn.marti.config.Federation.FederationOutgoing;
import com.bbn.marti.remote.groups.ConnectionInfo;
import com.bbn.marti.remote.groups.Direction;
import com.bbn.marti.remote.groups.Group;
import com.google.common.collect.Multimap;

public interface FederationManager {
	// Federate Configuration
	List<Federate> getAllFederates();
	Federate getFederate(@NotNull String federateUID);
	List<String> getGroupsInbound(@NotNull String federateUID);
	Multimap<String, String> getInboundGroupMap(@NotNull String federateUID);
	List<String> getGroupsOutbound(@NotNull String federateUID);
	List<String> getFederateRemoteGroups(String federateUID);
	List<String> getCAGroupsInbound(@NotNull String caID);
	List<String> getCAGroupsOutbound(@NotNull String caID);
	int getCAMaxHops(String caID);
	void addMaxHopsToCA(String caID, int maxHops);

	void addFederateToGroupsInbound(@NotNull String federateUID, @NotNull Set<String> localGroupNames);
	void addFederateToGroupsOutbound(@NotNull String federateUID, @NotNull Set<String> localGroupNames);
	void addFederateGroupsInboundMap(@NotNull String federateUID, @NotNull String remoteGroup, @NotNull String localGroup);
	void addInboundGroupToCA(@NotNull String caID, @NotNull Set<String> localGroupNames);
	void addOutboundGroupToCA(@NotNull String caID, @NotNull Set<String> localGroupNames);
	void removeFederateFromGroupsInbound(@NotNull String federateUID, @NotNull Set<String> localGroupNames);
	void removeFederateFromGroupsOutbound(@NotNull String federateUID, @NotNull Set<String> localGroupNames);
	void removeFederateInboundGroupsMap(@NotNull String federateUID, @NotNull String remoteGroup, @NotNull String localGroup);
	void removeInboundGroupFromCA(@NotNull String caID, @NotNull Set<String> localGroupNames);
	void removeOutboundGroupFromCA(@NotNull String caID, @NotNull Set<String> localGroupNames);
	void updateFederateMissionSettings(@NotNull String federateUID, @NotNull boolean missionFederateDefault, @NotNull List<Federation.Federate.Mission> federateMissions);

	// Get outgoing config object by address and port
	List<FederationOutgoing> getOutgoingConnections(@NotNull String address, int port);
	List<ConnectionStatus> getActiveConnectionInfo();
	// get the list of remote contacts from a given federate
	Collection<RemoteContact> getContactsForFederate(String federateUID, String groupVector);

	//Outgoing connections Configuration
	List<Federation.FederationOutgoing> getOutgoingConnections();
	// "disabled", "connected", "connecting" or message from most recent exception
	ConnectionStatus getOutgoingStatus(String name);

	void disableOutgoingForNode(String name);
	void disableOutgoing(String name);
	void disableOutgoing(Federation.FederationOutgoing outgoing);
	void enableOutgoing(String name);

	// will throw exception if output with same name already exists
	void addOutgoingConnection(String name, String host, int port, int reconnect, int maxRetries, boolean unlimitedRetries, boolean enable, int protocolVersion, String fallback, String token);
	void updateOutgoingConnection(Federation.FederationOutgoing original, Federation.FederationOutgoing update);
	void removeOutgoing(String name);

	List<X509Certificate> getCAList();
	void addCA(X509Certificate ca);
	void removeCA(X509Certificate ca);

	void updateFederateDetails(String federateId, boolean archive, boolean shareAlerts, boolean federatedGroupMapping, boolean automaticGroupMapping, boolean fallbackWhenNoGroupMappings, String notes, int maxHops);
	void removeFederate(String federateId);

	List<Federate> getConfiguredFederates();

	// Send ROL to messaging process, to be federated subject to group filtering.
	// Attach outbound groups to the ROL for federates using group mapping
	void submitFederateROL(ROL rol, NavigableSet<Group> groups);
	void submitMissionFederateROL(ROL rol, NavigableSet<Group> groups, String missionName);
	
	// Send ROL to messaging process, to be federated subject to group filtering.
	// Attach outbound groups to the ROL for federates using group mapping
	void submitFederateROL(ROL rol, NavigableSet<Group> groups, String fileHash);
	void submitMissionFederateROL(ROL rol, NavigableSet<Group> groups, String fileHash, String missionName);

	void reconfigureFederation();

	int incrementAndGetCounter();

	void updateFederationSubscriptionCache(ConnectionInfo connectionInfo, Federate federate);
	void clusterAddFederateUsersToGroups(String federateUID, Set<String> localGroupNames, Direction direction, boolean cluster);
	void clusterRemoveFederateUsersToGroups(String federateUID, Set<String> localGroupNames, Direction direction, boolean cluster);

	void trackConnectEventForFederate(String fedId, String fedName, boolean isRemote);
	void trackDisconnectEventForFederate(String fedId, String fedName, boolean isRemote);
	void trackSendChangesEventForFederate(String fedId, String fedName, boolean isRemote);
}
