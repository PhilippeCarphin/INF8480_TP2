package ca.polymtl.inf8480.tp2.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import ca.polymtl.inf8480.tp2.shared.ServerInterface;
import ca.polymtl.inf8480.tp2.shared.DispatcherInterface;

public class Client {

	private static String dispatcherIp = "127.0.0.1";
	private static String[] operationsList;

	private DispatcherInterface dispatcherStub = null;

	/**
	 * Parsing of arguments, creation of client object and delegation to
	 * dispatcher network object.
	 * @param args dispatcher ip and file containing list of operations
	 */
	public static void main(String[] args) {

		//TODO pouvoir lister les serveurs?

		parseArgs(args);
		Client client = new Client(dispatcherIp);
		try {
			int[] results = client.dispatcherStub.dispatchTasks(operationsList, "unsecured", "phil", "pipicaca");
			printResults(results);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Constructor.  Takes the ip of the dispatcher specified as a command
	 * line argument.
	 * @param dispIp IP of dispatcher
	 */
	public Client(String dispIp) {
		super();
		dispatcherStub = loadDispatcherStub(dispIp);
	}
	
	/**
	 * Print an array of integers.
	 * @param results array to print
	 */
	private static void printResults(int[] results) {
		for(int i = 0; i < results.length; ++i) {
			System.out.println("Results[" + String.valueOf(i) + "] = " + String.valueOf(results[i]));
		}
	}

	/**
	 * Get a network reference to a dispatcher object at the specified IP
	 * address.
	 * @param dispIp Ip of dispatcher
	 * @return stub for dispatcher
	 */
	private DispatcherInterface loadDispatcherStub(String dispIp) {
		DispatcherInterface stub = null;
		try {
			System.out.println("Loading dispatcher stub");
			Registry reg = LocateRegistry.getRegistry(dispIp);
			stub = (DispatcherInterface) reg.lookup("dispatcher");
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
	 * Get a network reference to a Server object at the specified IP
	 * address.
	 * @param hostname IP of server
	 * @return stub for server
	 */
	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			System.out.println("Calling LocateRegistry.getRegistry("
				+ hostname + ")");
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
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
	 * Parse command line arguments.  The first argument is the IP of the 
	 * dispatcher and the second argument is a file containing a list of 
	 * operations.
	 * @param args command line arguments
	 */
	private static void parseArgs(String[] args)
	{
		if (args.length == 2)
		{
			System.out.println("Using dispatcher ip : " + args[0]+ ".");
			dispatcherIp = args[0];
			System.out.println("Reading operations file : " + args[1] + ".\n");
			operationsList = readOps(args[1]);
		}
		else
		{
			System.out.println("Usage : ./client [dispatcher ip] [operations file]");
		}
	}

	/**
	 * Parse a file into an array of string objects.
	 * @param fileName file containing operations (one operation per line)
	 * @return Array of strings (one string per line of the file)
	 */
	private static String[] readOps(String fileName)
	{
		List<String> opList = new ArrayList<String>();

		try
		{
			FileReader fr = new FileReader(fileName);
			BufferedReader br = new BufferedReader(fr);
			String line;

			while((line = br.readLine()) != null)
			{
				opList.add(line);
			}
		}
		catch (FileNotFoundException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
		
		return opList.toArray(new String[opList.size()]);
	}

}
