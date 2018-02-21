package ca.polymtl.inf8480.tp1.server;

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

import ca.polymtl.inf8480.tp1.shared.ServerInterface;
import ca.polymtl.inf8480.tp1.shared.SyncedFile;
import ca.polymtl.inf8480.tp1.shared.Lock;
import sun.security.ssl.Debug;
import ca.polymtl.inf8480.tp1.shared.Response;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;

import java.io.FileReader;
import java.io.BufferedReader;

public class Server implements ServerInterface {

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	private static String FS_ROOT = "ajpcfs"; // Yet another new file system made by Alexandre Jouy and Philippe Carphin
	private static String FILE_STORE = FS_ROOT + "/" + "files";
	private static String LOCK_FILES = FS_ROOT + "/" + "lock";
	private static final String ID_FILENAME = FS_ROOT + "/" + "idFile.txt";

	private  ArrayList<Integer> idList = new ArrayList<Integer>(0);
	private File fileStore = null;
	private File lockFiles = null;
	private File idFile = null;

	private void createDirectories(){
		fileStore.getParentFile().mkdir();
		fileStore.mkdir();
		lockFiles.getParentFile().mkdir();
		lockFiles.mkdir();
		try {
			idFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Server() {
		super();

		fileStore = new File(FILE_STORE);
		lockFiles = new File(LOCK_FILES);
		idFile = new File(ID_FILENAME);


		createDirectories();
		try {
			readIDs();
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancer");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	@Override
	public int createClientID() throws RemoteException {
		int newID = getMaxId() + 1;
		addId(newID);
		showIds();
		return newID;
	}

	@Override
	public Response create(String nom) throws RemoteException {

		Response resp = new Response();
		try {

			File f = new File(FILE_STORE + "/" + nom);

			/*
			 * Cette fonction retourne false si le fichier existe deja.
			 * L'operation est dite atomique du point de vue des systemes de fichiers.
			 */
			boolean fileCreated = f.createNewFile();
			resp.retval = fileCreated;
			if(fileCreated){
				resp.message = "File created successfully\n";
			} else {
				resp.message = "Could not create file\n";
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return resp;
	}

	@Override
	public String[] list() throws RemoteException 
	{
		Path currentPath = Paths.get("").toAbsolutePath();
		String filesPath = currentPath.toString() + "/ajpcfs/files";
		String lockPath = currentPath.toString() + "/ajpcfs/lock";
		File filesFolder = new File(filesPath);

		File lockFolder = new File(lockPath);
		File[] allLocks = lockFolder.listFiles();

		File[] allFiles = filesFolder.listFiles();
		String[] filesNames = new String[allFiles.length];

		for (int i = 0; i < allFiles.length; i++)
		{
			filesNames[i] = allFiles[i].getName();

			for (File f : allLocks)
			{
				if (f.getName().equals(filesNames[i]))
				{
					try
					{
						String idStr;
						FileReader fr = new FileReader(lockPath + "/" + f.getName());
						BufferedReader br = new BufferedReader(fr);
						idStr = br.readLine();
						br.close();

						filesNames[i] = filesNames[i] + " (locked by client " + idStr + ")";
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}

		return filesNames;
	}

	@Override
	public SyncedFile[] syncLocalDirectory() throws RemoteException  
	{
		Path currentPath = Paths.get("").toAbsolutePath();
		String filesPath = currentPath.toString() + "/ajpcfs/files/";
		String[] filesNames = list();

		for (int i = 0; i < filesNames.length; i++)
		{
			filesNames[i] = filesNames[i].split(" ")[0];	
		}

		SyncedFile[] syncedFiles = new SyncedFile[filesNames.length];

		for (int i = 0; i < filesNames.length; i++)
		{
			syncedFiles[i] = new SyncedFile(filesPath + filesNames[i]);
		}

		return syncedFiles;
	}

	@Override
	public SyncedFile get(String nom, long checksum) throws RemoteException
	{
		Path currentPath = Paths.get("").toAbsolutePath();
		String filesPath = currentPath.toString() + "/ajpcfs/files/";
		String[] filesNames = list();

		for (int i = 0; i < filesNames.length; i++)
		{
			filesNames[i] = filesNames[i].split(" ")[0];	
		}

		for (int i = 0; i < filesNames.length; i++)
		{
			if (filesNames[i].equals(nom))	//On vérifie que le fichier demandé est present
			{
				File file = new File(filesPath + nom);
				long srvChecksum = file.lastModified();

				if (srvChecksum > checksum)	//on verifie si la version posedee par le serveur est la plus recente
				{
					return new SyncedFile(filesPath + nom);
				}
				else
				{
					return null;
				}
			}
		}

		return null;
	}

	@Override
	public Lock lock(String nom, int clientID, long checksum) throws RemoteException
	{
		String[] filesNames = list();

		for (int i = 0; i < filesNames.length; i++)
		{
			filesNames[i] = filesNames[i].split(" ")[0];	
		}

		Path currentPath = Paths.get("").toAbsolutePath();
		String lockPath = currentPath.toString() + "/ajpcfs/lock";
		File lockFolder = new File(lockPath);

		File[] allLocks = lockFolder.listFiles();

		for (String s : filesNames)	//On verifie que le fichier existe
		{
			if (nom.equals(s))
			{
				if (allLocks.length == 0)
				{
					try
					{
						PrintWriter writer = new PrintWriter(lockPath + "/" + s, "UTF-8");
						writer.println(clientID);
						writer.close();

						Lock lock = new Lock(clientID);

						File file = new File(currentPath.toString() + "/ajpcfs/files/" + s);
						long srvChecksum = file.lastModified();
		
						if (srvChecksum > checksum)	//on verifie si la version posedee par le serveur est la plus recente
						{
							lock.setSyncedFile(currentPath.toString() + "/ajpcfs/files/" + s);	//Si oui on envoie egalement le fichier
						}

						return lock;
					}
					catch (FileNotFoundException e)
					{
						e.printStackTrace();
						return null;
					}
					catch (UnsupportedEncodingException e)
					{
						e.printStackTrace();
						return null;
					}
				}
				else
				{
					for (File f : allLocks) //On verifie si le lock existe deja
					{
						if (f.getName().equals(s))	//Si oui
						{
							String idStr = null;

							try
							{
								FileReader fr = new FileReader(lockPath + "/" + s);
								BufferedReader br = new BufferedReader(fr);
								idStr = br.readLine();
								br.close();

								return new Lock(Integer.parseInt(idStr));	//On ne retourne que l id
							}
							catch (IOException e)
							{
								e.printStackTrace();
								return null;
							}
						}
						else	//Sinon on le cree
						{
							try
							{
								System.out.println("Creating lock");
								PrintWriter writer = new PrintWriter(lockPath + "/" + s, "UTF-8");
								writer.println(clientID);
								writer.close();

								Lock lock = new Lock(clientID);

								File file = new File(currentPath.toString() + "/ajpcfs/files/" + s);
								long srvChecksum = file.lastModified();
				
								if (srvChecksum > checksum)	//on verifie si la version posedee par le serveur est la plus recente
								{
									lock.setSyncedFile(currentPath.toString() + "/ajpcfs/files/" + s);	//Si oui on envoie egalement le fichier
								}

								return lock;
							}
							catch (FileNotFoundException e)
							{
								e.printStackTrace();
								return null;
							}
							catch (UnsupportedEncodingException e)
							{
								e.printStackTrace();
								return null;
							}
						}
					}
				}
			}	
		}
		return null;
	}

	@Override
	public boolean push(String nom, byte[] contenu, int clientID) throws RemoteException 
	{
		Path currentPath = Paths.get("").toAbsolutePath();
		String filesPath = currentPath.toString() + "/ajpcfs/files/";
		String lockPath = currentPath.toString() + "/ajpcfs/lock";
		File lockFolder = new File(lockPath);
		File[] allLocks = lockFolder.listFiles();
		String[] filesNames = list();

		for (int i = 0; i < filesNames.length; i++)
		{
			filesNames[i] = filesNames[i].split(" ")[0];	
		}

		for (File f : allLocks) //On verifie si le lock existe deja
		{
			if (f.getName().equals(nom))	//Si oui
			{
				String idStr = null;

				try
				{
					FileReader fr = new FileReader(lockPath + "/" + nom);
					BufferedReader br = new BufferedReader(fr);
					idStr = br.readLine();
					br.close();

					if (Integer.parseInt(idStr) == clientID)
					{
						SyncedFile pushedFile = new SyncedFile(nom, contenu);
						pushedFile.writeOnDisk(filesPath + nom);

						f.delete();	//On detruit le verrou apres ecriture du nouveau fichier

						return true;
					}
					else
					{
						return false;
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return false;
				}
			}
		}

		for (String s : filesNames)	//Si il n y a aucun verrou on verifie si le fichier est deja sur le server
		{
			if (s.equals(nom))
			{	
				return false;
			}
		}

		//Si ce n est pas le cas on l ecrit quand même
		SyncedFile pushedFile = new SyncedFile(nom, contenu);
		pushedFile.writeOnDisk(filesPath + nom);

		return true;
	}

	public void addId(int id)
	{
		BufferedWriter idWriter = null;
		FileWriter idFile = null;
		try {
			// The true here says we're in append mode
			idFile = new FileWriter(ID_FILENAME, true);
			idWriter = new BufferedWriter(idFile);
			idWriter.append(String.valueOf(id) + "\n");
			idWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try{
				idWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		idList.add(id);
	}

	public void readIDs() throws IOException
	{
		FileReader fr = new FileReader(ID_FILENAME);
		BufferedReader br = new BufferedReader(fr);
		StringBuffer sb = new StringBuffer();
		String line;
		while( (line = br.readLine()) != null){
			try
			{
				idList.add(Integer.parseInt(line));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
				return;
			}
		}
	}

	public boolean fileContainsID(Integer id)
	{
		boolean cont = idList.contains(id);
		if(cont)
			System.out.println("The id " + String.valueOf(id) + " is contained");
		else
			System.out.println("The id " + String.valueOf(id) + " is NOT contained");
		return idList.contains(id);
	}


	public void showIds()
	{
		System.out.println("========= Client IDs =============");
		for(Integer i : idList){
			System.out.println("Id : " + String.valueOf(i));
		}
	}

	public Integer getMaxId()
	{
		Integer max = -1;
		if(idList.size() != 0){
			max = Collections.max(idList);
		}
		return max;
	}
}
