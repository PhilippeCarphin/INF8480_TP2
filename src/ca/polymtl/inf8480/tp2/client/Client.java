package ca.polymtl.inf8480.tp2.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import ca.polymtl.inf8480.tp2.shared.ServerInterface;

public class Client {

	private static String dispatcherIp = "127.0.0.1";

	public static void main(String[] args) {

		//TODO ajouter comme arg l'adresse du dispatcher (et pouvoir lister les serveurs?)

		parseArgs(args);
	}

	private ServerInterface distantServerStub = null;
	private ServerInterface localServerStub = null;
	private ServerInterface serverStub = null;
	private static final boolean USE_DISTANT_SERVER = true;


	public Client(String distantServerHostname) {
		super();
		if( USE_DISTANT_SERVER ){
			if (distantServerHostname != null) {
				distantServerStub = loadServerStub(distantServerHostname);
				System.out.println("Called loadServerStub with hostname " + distantServerHostname + "\n");
			}
			serverStub = distantServerStub;
		} else {
			localServerStub = loadServerStub("127.0.0.1");
			serverStub = localServerStub;
		}
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
		if (args.length == 0)
		{
			System.out.println("Using default dispatcher ip (127.0.0.1).\n");
		}
		else if (args.length == 1)
		{
			System.out.println("Using dispatcher ip : " + args[0]+ ".\n");
			dispatcherIp = args[0];
		}
	}

}
