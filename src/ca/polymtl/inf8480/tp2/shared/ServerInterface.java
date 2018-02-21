package ca.polymtl.inf8480.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ca.polymtl.inf8480.tp1.shared.Response;
import ca.polymtl.inf8480.tp1.shared.Lock;


public interface ServerInterface extends Remote {
	int createClientID() throws RemoteException;
	Response create(String nom) throws RemoteException;
	String[] list() throws RemoteException;
	SyncedFile[] syncLocalDirectory() throws RemoteException;
	SyncedFile get(String nom, long checksum) throws RemoteException;
	Lock lock(String nom, int clientID, long checksum) throws RemoteException;
	boolean push(String nom, byte[] contenu, int clientID) throws RemoteException;
}
