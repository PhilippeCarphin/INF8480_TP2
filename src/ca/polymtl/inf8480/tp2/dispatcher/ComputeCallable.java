package ca.polymtl.inf8480.tp2.dispatcher;

import java.rmi.RemoteException;
import java.util.Arrays;
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
    		// Responsible for taking operations, and sending chunks of it to 
    		// to the server
    		// int[][] resultChunks = ???
    		// operations -> list<String[]> operationChunks;
    		// for(chunk in operationChunks){
    		//      compute and resend until receive value
    		//		result -> resultChunks
    		// }
    		// resultChunks -> results
    	
    		// Unsecured:
    		//
    		int serverCapacity = serverStub.getCapacity();
    		
        String[][] chunks = splitChunks(operations, serverCapacity);
        int[][] resultChunks = new int[chunks.length][];
        
        try
        {
        		int[] results = null;
        		for(int i = 0; i < chunks.length ; ++i) {
        			resultChunks[i] = serverStub.compute(chunks[i], mode, user, password).results;
            }
        		results = combineResults(resultChunks);

        		return results;
        }
        catch (RemoteException e)
        {
            throw new Exception();
        }
    }
    
    private String[][] splitChunks(String[] operations, int chunkLength){

    		int nbChunks = operations.length / chunkLength;
    		if( operations.length % chunkLength != 0) {
    			nbChunks += 1;
    		}
		String[][] chunks = new String[nbChunks][];
		for(int i = 0; i < nbChunks ; ++i) {
			chunks[i] = Arrays.copyOfRange(operations, i * chunkLength, Math.min((i+1)*chunkLength, operations.length));
		}
		return chunks;
    }
    
	private int[] combineResults(int[][] resultParts) {
		int nbParts = resultParts.length;
		int nbResults = 0;

		// Need to know how many results we have to allocate memory for
		// our concatenated array.
		for(int i = 0; i < nbParts ; ++i) {
			nbResults += resultParts[i].length;
		}

		int[] results = new int[nbResults];
		int offset = 0;
		for(int i = 0; i < nbParts ; ++i) {
			nbResults += resultParts[i].length;
			System.arraycopy(resultParts[i], 0, results, offset, resultParts[i].length);
			offset += resultParts[i].length;
		}

		return results;
	}
} 