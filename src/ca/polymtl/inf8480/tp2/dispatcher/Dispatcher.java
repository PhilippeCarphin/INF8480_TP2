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
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ca.polymtl.inf8480.tp2.shared.DispatcherInterface;
import ca.polymtl.inf8480.tp2.shared.ServerInterface;
import sun.security.ssl.Debug;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;

import java.io.FileReader;
import java.io.BufferedReader;

public class Dispatcher implements DispatcherInterface {

	private static String user = "";
	private static String password = "";

	public static void main(String[] args) {

		//TODO ajouter comme arg le mdp, pass (et pouvoir lister les serveurs?)
		parseArgs(args);

		Dispatcher dispatcher = new Dispatcher();
		dispatcher.run();
	}

	public Dispatcher() {
		super();
	}

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

	@Override
	public int[] dispatchTasks(String[] tasks, String mode, String user, String password) throws RemoteException
	{
		//TODO répartition des taches
		//TODO répartition des taches lors de pannes intempestives
		//TODO calculer la charge des serveurs
		//TODO appeler la classe compute callable pour créer les threads (ref : https://www.journaldev.com/1090/java-callable-future-example)
		//TODO créer un pool de thread de la taille du nombre de serveurs disponibles
		//TODO vérification de la justesse des calculs
		System.out.println("Received tasks to dispatch");
		return null;
	}

}
