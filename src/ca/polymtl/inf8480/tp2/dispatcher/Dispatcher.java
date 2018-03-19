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
	 * @param args
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
	 * @param ldapIp
	 * @return
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
	 * @param serverIp
	 * @return
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
	 * @param args
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
		System.out.println("Received tasks to dispatch");
		getServerStubs();

		String[][] parts = splitOperations(tasks);

		int[][] resultParts = dispatchInternal(parts);
		
		int[] results = combineResults(resultParts);

		//TODO vérification de la justesse des calculs >> spot check de quelques résultats reçus par le client montre que c'est bon.
		return results;
	}
	
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
	
	private int[][] dispatchInternal(String[][] operationLists){
		int nbLists = operationLists.length;
		int resultParts[][] = new int[nbLists][];
		ExecutorService executor = Executors.newFixedThreadPool(nbLists);
		ArrayList<Future<int[]>> futures = new ArrayList<Future<int[]>>();
		for(int i = 0; i < nbLists ; ++i) {
			ComputeCallable cc = new ComputeCallable(serverStubs[i], operationLists[i], "unsecured", "Phil", "password1234");
			Future<int[]> fut = executor.submit(cc);
			futures.add(fut);
			
		}
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


