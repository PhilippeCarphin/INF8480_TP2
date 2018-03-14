package ca.polymtl.inf8480.tp2.LDAP;

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

import ca.polymtl.inf8480.tp2.shared.LDAPInterface;
import ca.polymtl.inf8480.tp2.shared.ServerInterface;
import sun.security.ssl.Debug;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.io.FileReader;
import java.io.BufferedReader;

public class LDAP implements LDAPInterface {

    static HashMap<String, String> idMap = new HashMap<String, String>();
    static String[] servers = new String[3];

    public static void main(String[] args)
    {
        //TODO modifier les adresses des serveurs
        idMap.put("alice", "apassword");
        idMap.put("bob", "bpassword");

        servers[0] = "127.0.0.1";
        servers[1] = "127.0.0.1";
        servers[2] = "127.0.0.1";

		LDAP ldap = new LDAP();
		ldap.run();
	}

	public LDAP() {
		super();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			LDAPInterface stub = (LDAPInterface) UnicastRemoteObject.exportObject(this, 5012);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("LDAP", stub);
			System.out.println("LDAP ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
    }
    
    /* RMI methods */

    //Authentification d'un utilisateur auprès du serveur
    @Override
    public boolean authenticate(String user, String password) throws RemoteException
    {
        if (password.equals(idMap.get(user)))
            return true;
        else
            return false;
    }

    //Liste des serveurs
    @Override
    public String[] listServers() throws RemoteException
    {
        return servers;
    }

}
