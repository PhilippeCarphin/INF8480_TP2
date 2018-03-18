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
import ca.polymtl.inf8480.tp2.server.Operations;
import sun.security.ssl.Debug;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;

import java.io.FileReader;
import java.io.BufferedReader;

public class Server implements ServerInterface {

	private String LDAPHostname = "127.0.0.1";
	private LDAPInterface LDAPServerStub = null;
	private static Integer port = 5014; // Pas sur mais ça pourrait être utile.
	private static int errorRate = 0;

	public static void main(String[] args) {
		parseArgs(args);

		Server server = new Server();
		server.run();
	}

	public Server() {
		super();
		// LDAPServerStub = loadLDAPStub(LDAPHostname);
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, port);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

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

	//On parse le taux d'erreurs qu'on désire au lancement du serveur de calcul
	private static void parseArgs(String[] args)
	{
		if (args.length < 1) {
			System.out.println("You need to pass an argument to determine the error rate (0 - 100).\n");
		} else {
			errorRate = Integer.parseInt(args[0]);
			if (args.length < 2) {
				port = 5014;
			} else {
				port = Integer.parseInt(args[1]);
			}
		}
	}

	@Override
	public int[] compute(String[] operations, String mode, String user, String password) throws RemoteException
	{
		System.out.println("Server received request, mode=" + mode + ", user=" + user + ", password=" + password);
		int opNum = operations.length;
		int[] results = new int[opNum];

		//TODO calcul du nombre de taches et refus éventuel

		if (mode.equals("secured"))
		{
			//Authenticate to the LDAP server, throws exceptions if anything happens
			try
			{
				if (!LDAPServerStub.authenticate(user, password))
					return null;
			}
			catch (RemoteException e)
			{
				throw new RemoteException();
			}

			for (int i = 0; i < opNum; i++)
			{
				if (operations[i].split(" ")[0].equals("prime"))
				{
					results[i] = Operations.prime(Integer.parseInt(operations[i].split(" ")[1]));
				}
				else
				{
					results[i] = Operations.pell(Integer.parseInt(operations[i].split(" ")[1]));
				}
			}
		}
		else
		{
			//On calcule quand même la valeur pour avoir un temps de calcul identique qu'il y ait des erreurs ou non
			for (int i = 0; i < opNum; i++)
			{
				if (operations[i].split(" ")[0].equals("prime"))
				{
					results[i] = Operations.prime(Integer.parseInt(operations[i].split(" ")[1]));
				}
				else
				{
					results[i] = Operations.pell(Integer.parseInt(operations[i].split(" ")[1]));
				}

				Random rand = new Random();
				int  n = rand.nextInt(100) + 1;

				if (n <= errorRate)
				{
					rand = new Random();
					results[i] = rand.nextInt(4001);
				}
			}
		}

		return results;
	}

}
