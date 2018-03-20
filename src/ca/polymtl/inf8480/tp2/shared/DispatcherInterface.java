package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface DispatcherInterface extends Remote {
    Response dispatchTasks(String[] tasks, String mode, String user, String password) throws RemoteException;
}
