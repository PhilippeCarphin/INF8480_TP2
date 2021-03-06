package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ServerInterface extends Remote {
    Response compute(String[] operations, String mode, String user, String password) throws RemoteException;
    int getCapacity() throws RemoteException;
}
