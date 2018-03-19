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

	public static void main(String[] args) {

		//TODO pouvoir lister les serveurs?

		parseArgs(args);
		Client client = new Client(dispatcherIp);
		String [] tasks = {"Allo", "bonjour"};
		String mode = "test";
		String user = "phil";
		String password = "gobonjourpipicaca";
		try {
			client.dispatcherStub.dispatchTasks(operationsList, mode, user, password);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private ServerInterface distantServerStub = null;
	private ServerInterface localServerStub = null;
	private ServerInterface serverStub = null;
	private DispatcherInterface dispatcherStub = null;
	private static final boolean USE_DISTANT_SERVER = true;


	public Client(String dispIp) {
		super();
		dispatcherStub = loadDispatcherStub(dispIp);
	}
	
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

	private static void parseArgs(String[] args)
	{
		//On parse l'ip du dispatcher et le fichier d'opérations à lire
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

	private static String[] readOps(String fileName)
	{
		List<String> opList = new ArrayList<String>();

		//On lit le fichier ligne par ligne pour obtenir la liste des opérations
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
