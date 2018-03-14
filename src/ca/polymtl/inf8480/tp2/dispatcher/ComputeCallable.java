package ca.polymtl.inf8480.tp2.dispatcher;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ca.polymtl.inf8480.tp2.shared.ServerInterface;

public class ComputeCallable implements Callable<int[]>
{
    private String[] operations;
    private String mode;
    private String user;
    private String password;
    private ServerInterface serverStub;

    public ComputeCallable(ServerInterface serverStub, String[] operations, String mode, String user, String password)
    {
        this.serverStub = serverStub;
        this.operations = operations;
        this.mode = mode;
        this.user = user;
        this.password = password;
    }

    @Override
    public int[] call() throws Exception
    {
        int[] results;

        try
        {
            results = serverStub.compute(operations, mode, user, password);
            return results;
        }
        catch (RemoteException e)
        {
            throw new Exception();
        }
    }
} 