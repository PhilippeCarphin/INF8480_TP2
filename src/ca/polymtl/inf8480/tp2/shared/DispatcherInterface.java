package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface DispatcherInterface extends Remote {
    int[] dispatchTasks(String[] tasks) throws RemoteException;
}
