package ca.polymtl.inf8480.tp2.dispatcher;

import ca.polymtl.inf8480.tp2.shared.AuthenticationException;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ca.polymtl.inf8480.tp2.shared.ServerInterface;
import ca.polymtl.inf8480.tp2.shared.Response;

public class ComputeCallable implements Callable<Response>
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
    /**
     * Does all the work of interacting with the server.
     */
    public Response call() throws Exception, AuthenticationException
    {
    		if(mode.equals("secured")) {
    			return callSecured();
    		} else {
    			return callSecured(); // Change this of course
    		}
    }
    
    private Response callUnsecured() throws Exception, AuthenticationException
    {
    		Response resp = new Response();
    		
    		try {
    			resp = serverStub.compute(operations, "unsecured", user, password);
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}
    		
    		return resp;
    }
    
    private Response callSecured() throws Exception, AuthenticationException
    {
	    	Response resp = new Response();
	    	
		int serverCapacity = serverStub.getCapacity();
	    String[][] chunks = splitChunks(operations, serverCapacity);
	    int[][] resultChunks = new int[chunks.length][];
	    
	    try
	    {
	    		int[] results = null;
	    		for(int i = 0; i < chunks.length ; ++i) {
	    			Response subResp = serverStub.compute(chunks[i], "secured", user, password);
	    			if (subResp.code == Response.Code.NO_ERROR) {
	        			resultChunks[i] = subResp.results;
	    			} else if (subResp.code == Response.Code.AUTH_FAILURE) {
	    				return subResp;
	    			}
	        }
	    		resp.results = combineResults(resultChunks);
	    		return resp;
	    }
	    catch (RemoteException e)
	    {
	        throw e;
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