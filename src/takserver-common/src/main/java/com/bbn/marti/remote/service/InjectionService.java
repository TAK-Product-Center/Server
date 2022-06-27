package com.bbn.marti.remote.service;

import java.rmi.RemoteException;
import java.util.Set;

import com.bbn.marti.remote.injector.InjectorConfig;

/**
 */
public interface InjectionService {

	 public boolean setInjector(String uid, String toInject);
	 
	 public InjectorConfig deleteInjector(InjectorConfig injector) throws RemoteException;
	 
	 public Set<InjectorConfig> getInjectors(String uid);
	 
	 public Set<InjectorConfig> getAllInjectors();
}
