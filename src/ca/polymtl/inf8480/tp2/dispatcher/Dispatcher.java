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
	public int[] dispatchTasks(String[] tasks, String mode, String user, String password) throws RemoteException
	{
		//TODO calculer des sous parties de la bonne taille pour qu'elles puissent être acceptées par les serveurs de calcul
		//TODO re-dispatcher les taches si elles ont été refusées
		//!\ le dispatcher connais le taux de refus de chacun des serveurs
		//TODO pour le mode non sécurisé, envoyer les même opérations à tous les serveurs puis comparer les résultats
		System.out.println("Received tasks to dispatch");
		getServerStubs();

		String[][] parts = splitOperations(tasks);

		int[][] resultParts = null;
		if(mode.equals("secured")) {
			resultParts = dispatchInternalSecured(parts, user, password);
		}

		int[] results = combineResults(resultParts);

		//TODO vérification de la justesse des calculs >> spot check de quelques résultats reçus par le client montre que c'est bon.
		return results;
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
	private int[][] dispatchInternalSecured(String[][] operationLists, String user, String password){
		
		// Based on server capacities, split into a number of parts
		// equal to the number of servers.
		// For each server, the compute callable will work out how to
		// send chunks to their server and will be responsible for the
		// whole thing
		// So it will get string[] and return a int[]
		int nbLists = operationLists.length;
		int resultParts[][] = new int[nbLists][];

		ExecutorService executor = Executors.newFixedThreadPool(nbLists);
		ArrayList<Future<int[]>> futures = new ArrayList<Future<int[]>>();

		// Dispatch work
		for(int i = 0; i < nbLists ; ++i) {
			ComputeCallable cc = new ComputeCallable(serverStubs[i], operationLists[i], "secured", user, password);
			Future<int[]> fut = executor.submit(cc);
			futures.add(fut);

		}

		// Wait for results
		for(int i = 0; i < nbLists; ++i) {
			try {
				resultParts[i] = futures.get(i).get();
			} catch (ExecutionException | InterruptedException e) {
				//TODO répartition des taches lors de pannes intempestives
				e.printStackTrace();
			}
		}

		executor.shutdown();

		return resultParts;
	}
	
	private int[] dispatchInternalUnsecured(String[] operations) {
		// Splitter en bouché de serveurs
		String [][] smallChunks = splitIntoSmallChunks(operations);
		int[][] smallResults = new int[smallChunks.length][];
		// Pour chaque bouchée
		//      getResultUnsecured(bouchée)
		// Rassembler les bouchées
		return combineResults(smallResults);
	}
	
	private String[][] splitIntoSmallChunks(String[] operations){
		return new String[1][1];
	}
	
	private int[] getResultsUnsecured(String[] smallChunk) {
		boolean straightAnswerFound = false;
		int straightAnswer[] = null;
		while(!straightAnswerFound) {
			int[][] answers = new int[serverStubs.length][];
			// Dispatch work with compute callables
		
			// Wait for results
			
			// 
			straightAnswer = getCredibleAnswers(answers);
			
		}
		return new int[1];
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
		return new int[1];
	}
	
	private int getCredibleAnswer(int[] operAnswers) {
		// If there are two equal values in the array,
		// that's the credible answer.
		return 0;
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


