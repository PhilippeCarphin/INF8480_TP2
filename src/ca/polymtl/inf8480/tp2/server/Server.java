package ca.polymtl.inf8480.tp2.server;

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

import ca.polymtl.inf8480.tp2.shared.ServerInterface;
import ca.polymtl.inf8480.tp2.server.Operations;
import sun.security.ssl.Debug;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;

import java.io.FileReader;
import java.io.BufferedReader;

public class Server implements ServerInterface {

	public static void main(String[] args) {
		//TODO ajouter paramètre de fiabilité (% de fois où le serveur va renvoyer un résultat faux)

		Server server = new Server();
		server.run();
	}

	public Server() {
		super();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 0);

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

	@Override
	public int[] compute(String[] operations, String mode, String user, String password) throws RemoteException
	{
		int opNum = operations.length;
		int[] results = new int[opNum];

		//TODO calcul du nombre de taches et refus éventuel
		//TODO mode non sécurisé
		//TODO authentifier le répartiteur auprès du LDAP

		if (mode.equals("secured"))
		{
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

		return results;
	}

}
