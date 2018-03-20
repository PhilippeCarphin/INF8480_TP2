package ca.polymtl.inf8480.tp2.dispatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ca.polymtl.inf8480.tp2.shared.DispatcherInterface;
import ca.polymtl.inf8480.tp2.shared.ServerInterface;
import ca.polymtl.inf8480.tp2.shared.LDAPInterface;
import ca.polymtl.inf8480.tp2.shared.Response;
import ca.polymtl.inf8480.tp2.shared.AuthenticationException;
import sun.security.ssl.Debug;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.io.FileReader;
import java.io.BufferedReader;

public class Dispatcher implements DispatcherInterface {

	private static String user = "";
	private static String password = "";

	/**
	 * Parse command line arguments and run
	 * @param args : command line arguments
	 */
	public static void main(String[] args) {

		//TODO ajouter comme arg le mdp, pass (et pouvoir lister les serveurs?)
		parseArgs(args);

		Dispatcher dispatcher = new Dispatcher();
		dispatcher.run();
	}

	/**
	 * Constructor, get a reference to the LDAP server running
	 * on the same machine.
	 */
	public Dispatcher() {
		super();
		ldapStub = loadLdapStub("127.0.0.1");
	}

	private LDAPInterface ldapStub = null;
	private ServerInterface[] serverStubs = null;

	/**
	 * Get a network reference to an LDAP at the specified IP.
	 * @param ldapIp Ip of LDAP server
	 * @return stub for LDAP server
	 */
	private LDAPInterface loadLdapStub(String ldapIp) {
		LDAPInterface stub = null;
		try {
			System.out.println("Loading LDAP stub");
			Registry reg = LocateRegistry.getRegistry(ldapIp);
			stub = (LDAPInterface) reg.lookup("LDAP");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas defini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
		return stub;
	}

	/**
	 * Get a reference to a network server object from the specified
	 * IP address.
	 * @param serverIp IP adress of server
	 * @return stub for server
	 */
	private ServerInterface loadServerStub(String serverIp) {
		ServerInterface stub = null;
		try {
			System.out.println("Loading LDAP stub");
			Registry reg = LocateRegistry.getRegistry(serverIp);
			stub = (ServerInterface) reg.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas defini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
		return stub;
	}

	/**
	 * Run method for this remote object.
	 */
	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			DispatcherInterface stub = (DispatcherInterface) UnicastRemoteObject.exportObject(this, 5012);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("dispatcher", stub);
			System.out.println("Dispatcher ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	/**
	 * Parsing of arguments for dispatcher.  Either there are no arguments and
	 * we will run in unsecured mode.
	 * Otherwise there must be two arguments: username and password.
	 * @param args Command line arguments
	 */
	private static void parseArgs(String[] args)
	{
		//On parse les arguments au répartiteur pour choisir son mode de fonctionnement
		if (args.length == 0)
			System.out.println("You did not pass any arguments to the dispatcher so it can only works in unsecured mode.\n");
		else if (args.length == 2)
		{
			System.out.println("Passing username and password to the dispatcher. It can works in secured and unsecured mode.\n");
			user = args[0];
			password = args[1];
		}
		else
		{
			System.out.println("You need to pass either 2 arguments (username and password) or 0.\n");
			System.exit(1);
		}
	}

	/**
	 * Dispatch tasks among available servers.  The list of available servers
	 * is obtained from the LDAP service.  Operations are distributed among the
	 * available servers, the results are collected and returned to the clien.
	 */
	@Override
	public Response dispatchTasks(String[] tasks, String mode, String user, String password) throws RemoteException
	{
		Response resp = new Response();
		//TODO calculer des sous parties de la bonne taille pour qu'elles puissent être acceptées par les serveurs de calcul
		//TODO re-dispatcher les taches si elles ont été refusées
		//!\ le dispatcher connais le taux de refus de chacun des serveurs
		//TODO pour le mode non sécurisé, envoyer les même opérations à tous les serveurs puis comparer les résultats
		System.out.println("Received tasks to dispatch");
		getServerStubs();

		if(mode.equals("secured")) {
			String[][] parts = splitOperations(tasks);
			int[][] resultParts = null;
			try {
				resultParts = dispatchInternalSecured(parts, user, password);
			} catch (AuthenticationException e) {
				resp.code = Response.Code.AUTH_FAILURE;
				return resp;
			}
			int[] results = combineResults(resultParts);
			resp.results = results;
		} else {
			int[] results = null;
			try {
				results = dispatchInternalUnsecured(tasks);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			resp.results = results;
		}

		return resp;
	}

	/**
	 * Subroutine to split list of operations into sublists.  This method will
	 * consider the amount of resources available to each server.
	 * @param operations Operations to run
	 * @return results of operations
	 */
	private String[][] splitOperations(String[] operations){

		int nbParts = serverStubs.length;
		//TODO calculer la charge des serveurs
		//TODO répartition des taches selon la charge
		int partLength = operations.length / nbParts;
		String[][] parts = new String[serverStubs.length][];
		for(int i = 0; i < nbParts ; ++i) {
			parts[i] = Arrays.copyOfRange(operations, i * partLength, Math.min((i+1)*partLength, operations.length));
		}
		return parts;
	}

	/**
	 * Internal details of dispatching.  This method creates threads to dispatch
	 * the work concurrently to all servers and wait for all servers to send their
	 * answer.
	 * @param operationLists Lists of operations to send to individual servers
	 * @return arrays of results from individual servers
	 */
	private int[][] dispatchInternalSecured(String[][] operationLists, String user, String password) throws AuthenticationException {
		
		// Based on server capacities, split into a number of parts
		// equal to the number of servers.
		// For each server, the compute callable will work out how to
		// send chunks to their server and will be responsible for the
		// whole thing
		// So it will get string[] and return a int[]
		int nbLists = operationLists.length;
		int resultParts[][] = new int[nbLists][];

		ExecutorService executor = Executors.newFixedThreadPool(nbLists);
		ArrayList<Future<Response>> futures = new ArrayList<Future<Response>>();

		// Dispatch work
		for(int i = 0; i < nbLists ; ++i) {
			ComputeCallable cc = new ComputeCallable(serverStubs[i], operationLists[i], "secured", user, password);
			Future<Response> fut = executor.submit(cc);
			futures.add(fut);

		}

		// Wait for results
		for(int i = 0; i < nbLists; ++i) {
			try {
				Response subResp = futures.get(i).get();
				if(subResp.code == Response.Code.AUTH_FAILURE) {
					throw new AuthenticationException("Authentication failed with some server");
				}
				resultParts[i] = subResp.results;
			} catch (ExecutionException | InterruptedException e) {
				//TODO répartition des taches lors de pannes intempestives
				e.printStackTrace();
			}
		}

		executor.shutdown();
		return resultParts;
	}
	
	private int[] dispatchInternalUnsecured(String[] operations) throws RemoteException {
		// Splitter en bouché de serveurs
		String [][] smallChunks = splitIntoChunks(operations);
		int[][] chunkResults = new int[smallChunks.length][];
		int nbChunks = smallChunks.length;
		for(int i = 0; i < nbChunks ; i++) {
			chunkResults[i] = getResultsUnsecured(smallChunks[i]).results;
		}
		return combineResults(chunkResults);
	}
	
	private String[][] splitIntoChunks(String[] operations) throws RemoteException {
		return Util.splitChunks(operations, minServerCapacity());
	}
	
	private int minServerCapacity() throws RemoteException {
		int minCapacity = Integer.MAX_VALUE;
		for(ServerInterface s : serverStubs) {
			int serverCapacity = s.getCapacity();
			if (serverCapacity < minCapacity) {
				minCapacity = serverCapacity;
			}
		}
		System.out.println("minCapcity() returning " + String.valueOf(minCapacity));
		return minCapacity;
	}
	
	private Response getResultsUnsecured(String[] smallChunk) {
		System.out.println("getResultsUnsecured()");
		Response resp = new Response();
		boolean straightAnswerFound = false;
		int straightAnswer[] = null;
		while(!straightAnswerFound) {
			int[][] answers = new int[serverStubs.length][];
			//==============================================================
			int nbServers = serverStubs.length;
			ExecutorService executor = Executors.newFixedThreadPool(nbServers);
			ArrayList<Future<Response>> futures = new ArrayList<Future<Response>>();

			// Dispatch work
			for(int i = 0; i < nbServers ; ++i) {
				ComputeCallable cc = new ComputeCallable(serverStubs[i], smallChunk, "unsecured", user, password);
				Future<Response> fut = executor.submit(cc);
				futures.add(fut);
			}

			// Wait for results
			for(int i = 0; i < nbServers; ++i) {
				try {
					Response subResp = futures.get(i).get();
					answers[i] = subResp.results;
				} catch (ExecutionException | InterruptedException e) {
					//TODO répartition des taches lors de pannes intempestives
					e.printStackTrace();
				}
			}
			// ==============================================================
			straightAnswer = getCredibleAnswers(answers);
			if(straightAnswer != null) {
				straightAnswerFound = true;
			}
		}
		resp.results = straightAnswer;
		return resp;
	}
	
	private int[] getCredibleAnswers(int[][] answers) {
		int nbOps = answers[0].length;
		int nbServers = answers.length;
		int[] credibleAnswers = new int[nbOps];
		for(int oper = 0; oper < nbOps ; ++oper) {
			int[] operAnswers = new int[nbServers];
			for(int server = 0; server < nbServers; ++server) {
				operAnswers[server] = answers[server][oper];
			}
			credibleAnswers[oper] = getCredibleAnswer(operAnswers);
			
		}
		return credibleAnswers;
	}
	
	private int getCredibleAnswer(int[] operAnswers) {
		// If there are two equal values in the array,
		// that's the credible answer.
		// TODO Complete this bastard
		return operAnswers[0];
	}

	/**
	 * Helper method to take concatenate an array of array of int into a
	 * single array of int to return to the client.
	 * @param resultParts array of array of ints
	 * @return concatenation of arrays
	 */
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

	/**
	 * Obtain list of network objects for the servers.  The list of server IP
	 * addresses is obtained from LDAP and a network object is created from
	 * each of these IPs.
	 */
	public void getServerStubs() {
		String[] servers = null;
		try {
			servers = ldapStub.listServers();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		serverStubs = new ServerInterface[servers.length];
		for(int i = 0; i < servers.length ; ++i) {
			System.out.println("Servers[" + String.valueOf(i) + "] : " + servers[i]);
			serverStubs[i] = loadServerStub(servers[i]);
		}
	}

	/**
	 * This method can be used to test interactions with LDAP and servers without
	 * depending on arguments coming from the client.
	 */
	public void testDispatch() {
		String[] operations = {"a", "b"};
		for(int i = 0; i < serverStubs.length ; ++i) {
			try {
				serverStubs[i].compute(operations,"test", "phil", "pipicaca");
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}


