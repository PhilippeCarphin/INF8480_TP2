package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface LDAPInterface extends Remote {
    boolean authenticate(String user, String password) throws RemoteException;
    String[] listServers() throws RemoteException;
}
