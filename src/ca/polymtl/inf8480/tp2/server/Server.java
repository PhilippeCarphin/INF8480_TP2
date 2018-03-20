package ca.polymtl.inf8480.tp2.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

import ca.polymtl.inf8480.tp2.shared.ServerInterface;
import ca.polymtl.inf8480.tp2.shared.LDAPInterface;
import ca.polymtl.inf8480.tp2.shared.Response;
import ca.polymtl.inf8480.tp2.server.Operations;
import sun.security.ssl.Debug;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;

import java.io.FileReader;
import java.io.BufferedReader;

public class Server implements ServerInterface {

	private static String LDAPHostname = "127.0.0.1";
	private LDAPInterface LDAPServerStub = null;
	private static int DEFAULT_NB_OPS_GUARANTEE = 3;
	private static int nbOpsGuarantee = DEFAULT_NB_OPS_GUARANTEE;
	private static Integer port = 5014; // Pas sur mais ça pourrait être utile.
	private static int errorRate = 0;

	/**
	 * Parsing of arguments and call of run method
	 * @param args Command line arguments (see parseArgs)
	 */
	public static void main(String[] args) {
		parseArgs(args);

		Server server = new Server();
		server.run();
	}

	/**
	 * Constructor.  For testing purposes, the server doesn't use LDAP because
	 * the external servers can't connect to my computer.
	 */
	public Server() {
		super();
		LDAPServerStub = loadLDAPStub(LDAPHostname);
	}

	/**
	 * Run method of remote object.
	 */
	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, port);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready. Capacity = " + String.valueOf(nbOpsGuarantee));
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	/**
	 * Get a network reference to an LDAP object from the specified
	 * IP address.
	 * @param hostname Adress of LDAP service
	 * @return stub for LDAP service
	 */
	private LDAPInterface loadLDAPStub(String hostname) {
		LDAPInterface stub = null;

		try {
			System.out.println("Calling LocateRegistry.getRegistry("+ hostname + ")\n");
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (LDAPInterface) registry.lookup("LDAP");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()+ "' n'est pas defini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	/**
	 * Parsing of arguments.
	 * First argument is the error rate that this server should simulate.
	 * Second argument is a port for the server to run on.  May be useful
	 * later.
	 * @param args : error rate and optional port number to run on
	 */
	private static void parseArgs(String[] args)
	{
		if (args.length < 1) {
			System.out.println("You need to pass an argument to determine the error rate (0 - 100).\n");
		} else {
			errorRate = Integer.parseInt(args[0]);
			if (args.length > 1) {
				nbOpsGuarantee = Integer.parseInt(args[1]);
				if (args.length > 2) {
					LDAPHostname = args[2];
				}
			}
		}
	}
	
	private static void printArgs(String[] args) {
		System.out.println("Command line arguments : ");
		for(int i = 0; i < args.length; ++i) {
			System.out.println("   args[" + String.valueOf(i) + "] = " + args[i]);
		}
	}

	@Override
	public Response compute(String[] operations, String mode, String user, String password) throws RemoteException
	{
		Response resp = new Response();
		System.out.println("Server received request, mode=" + mode + ", user=" + user + ", password=" + password);
		System.out.println("Number of operation = " + String.valueOf(operations.length));
		if(refuseWork(operations.length)) {
			System.out.println("Refusing to work based on operations size");
			return null;//TODO calcul du nombre de taches et refus éventuel
		}

		//TODO Ajouter la vérification du taux de refus
		if (mode.equals("secured"))
		{
			// Authenticate to the LDAP server, throws exceptions if anything happens
			try {
				if (!LDAPServerStub.authenticate(user, password)) {
					System.out.println("Could not authenticate user");
					resp.code = Response.Code.AUTH_FAILURE;
				}
			} catch (RemoteException e) {
				throw new RemoteException();
			}
			resp.results = computeInternal(operations, false);
		} else {
			resp.results = computeInternal(operations, true);
		}
		return resp;
	}
	
	@Override
	public int getCapacity() {
		return nbOpsGuarantee;
	}

	/**
	 * Compute the result of a list of operations specified by strings.
	 * The computations can be sprinkled with synthetic errors at a rate
	 * specified by errorRate (received as an argument at server launch time).
	 * @param operations list of operations
	 * @param withErrors specify whether to generate synthetic errors
	 * @return array of results of operations
	 */
	private int[] computeInternal(String[] operations, Boolean malicious) {
		int[] results = new int[operations.length];
		for (int i = 0; i < operations.length ; i++)
		{
			// On calcule quand même la valeur pour avoir un temps
			// de calcul identique qu'il y ait des erreurs ou non
			results[i] = computeOperation(operations[i]);
			
			if(malicious) {
				Random rand = new Random();
				int  n = rand.nextInt(100);
	
				if (n < errorRate)
				{
					rand = new Random();
					results[i] = rand.nextInt(4001);
				}
			}
		}
		return results;
	}

	private Boolean refuseWork(int nbOperations) {
		if(nbOperations < nbOpsGuarantee) {
			return false;
		} else {
			Random rand = new Random();
			float chancesToRefuse = (float) ((nbOperations - nbOpsGuarantee)/(5.0*nbOpsGuarantee));
			float f = rand.nextFloat();
			return (f < chancesToRefuse);
		}
	}
	/**
	 * Compute the result of an operation specified by a string.  The first
	 * word of the string is the name of an operation and the second word
	 * is the argument.
	 * @param operation A single operation
	 * @return return value of operation
	 */
	private static int computeOperation(String operation)
	{
		String[] words = operation.split(" ");
		String opName = words[0];
		Integer arg = Integer.parseInt(words[1]);
		if (opName.equals("prime")) {
			return Operations.prime(arg);
		} else {
			return Operations.pell(arg);
		}
	}

}
