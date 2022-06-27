package com.bbn.marti.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FederationConfigInterface extends Remote {
    FederationConfigInfo getFederationConfig() throws RemoteException;
    void modifyFederationConfig(FederationConfigInfo info) throws RemoteException;
    boolean verifyFederationTruststore() throws RemoteException;
}
