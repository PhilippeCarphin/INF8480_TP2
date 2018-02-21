package ca.polymtl.inf8480.tp1.client;

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

import ca.polymtl.inf8480.tp1.shared.ServerInterface;
import ca.polymtl.inf8480.tp1.shared.SyncedFile;
import ca.polymtl.inf8480.tp1.shared.Response;
import ca.polymtl.inf8480.tp1.shared.Lock;

public class Client {

	public static void main(String[] args) {
		if( args.length <= 1 )
		{
			//client.runTests();
			System.out.println("\nUsage : client hostname command [arg]\n");
		}
		else
		{
			String distantHostname = args[0];
			Client client = new Client(distantHostname);

			client.parseArgs(args);
			client.runCmd();
		}
	}
	private enum Command {CREATE, LIST, GET, SYNC, LOCK, PUSH}
	private ServerInterface distantServerStub = null;
	private ServerInterface localServerStub = null;
	private ServerInterface serverStub = null;
	private static final boolean USE_DISTANT_SERVER = true;
	private Command command = null;
	private String commandStr = null;
	private String argument = null;
	private static final String ID_FILENAME = "clientid.txt";
	private int clientID = -1;

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

	public void runTests()
	{
		// testCreate(localServerStub);
		testCreateClientID();
		testMethod(localServerStub);
	}

	public void testMethod(ServerInterface si)
	{

	}

	public void testCreate(ServerInterface si)
	{
		try {
			si.create("fichier_test");
		} catch (RemoteException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private void testCreateClientID()
	{
		getClientID();
	}

	public void runCmd()
	{
		switch(command){
			case CREATE:
				runCreate();
				break;
			case GET:
				runGet();
				break;
			case LIST:
				runList();
				break;
			case SYNC:
				runSync();
				break;
			case LOCK:
				runLock();
				break;
			case PUSH:
				runPush();
				break;
			default:
				System.out.println("Command " + commandStr + " not yet implemented.");
				System.exit(1);
				break;
		}
	}

	private void runCreate()
	{
		Response resp = null;
		try {
			resp = serverStub.create(argument);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println(resp.message);
	}

	private void runList()
	{
		try
		{
			String[] files = serverStub.list();

			System.out.println("\nDistant files :\n");

			for (String s : files) {
				System.out.println(s);
			}
		}
		catch (RemoteException e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}	
	}

	private void runGet()
	{
		try
		{
			Path currentPath = Paths.get("").toAbsolutePath();
			File f = new File(currentPath + "/" + argument);
			SyncedFile syncedFile;

			if (f.exists())
			{
				long fileChecksum = f.lastModified();
				syncedFile =  serverStub.get(argument, fileChecksum);
			}
			else
			{
				syncedFile =  serverStub.get(argument, 0);
			}

			if (syncedFile == null)
			{
				System.out.println("\nThe file you are trying to fetch is already up to date or does not exists on the server.\n");
			}
			else
			{
				System.out.println("\n" + argument + " fetched and is now at its newest version.\n");
				syncedFile.writeOnDisk(f.getAbsolutePath());
			}
		}
		catch (RemoteException e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private void runSync()
	{
		try
		{
			String[] files = serverStub.list();
			Path currentPath = Paths.get("").toAbsolutePath();
			SyncedFile syncedFile;

			for (String s : files)
			{
				syncedFile =  serverStub.get(s, 0);
				syncedFile.writeOnDisk(currentPath.toString() + "/" + s);
				System.out.println(s + " synced.");
			}
		}
		catch (RemoteException e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}	
	}

	private void runLock()
	{
		try
		{
			Path currentPath = Paths.get("").toAbsolutePath();
			File f = new File(currentPath + "/" + argument);
			File idFile = new File(currentPath + "/" + ID_FILENAME);
			Lock lock;
			SyncedFile syncedFile;


			if (idFile.exists())
			{
				readIdFromFile();
			}
			else
			{
				System.out.println("\nYou first need to get an ID from the server to be able to lock a file.\n");
				return;
			}

			if (f.exists())
			{
				long fileChecksum = f.lastModified();
				lock =  serverStub.lock(argument, clientID, fileChecksum);
			}
			else
			{
				lock =  serverStub.lock(argument, clientID, 0);
			}

			if (lock == null)
			{
				System.out.println("\nAn error occured or the file requested does not exist.\n");
			}
			else
			{
				syncedFile = lock.getSyncedFile();

				if (syncedFile == null)
				{
					if (clientID == lock.getLockID())
					{
						System.out.println("\nYou locked the file " + argument + " and you already own the newest version.\n");
					}
					else
					{
						System.out.println("\nThe file " + argument + " is already locked by the client " + String.valueOf(lock.getLockID()) + ".\n");
					}
				}
				else
				{
					syncedFile.writeOnDisk(f.getAbsolutePath());
					System.out.println("\nYou locked the file " + argument + " and fetched the latest version.\n");
				}
			}
		}
		catch (RemoteException e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
		}	
	}

	private void runPush()
	{
		Path currentPath = Paths.get("").toAbsolutePath();
		File idFile = new File(currentPath + "/" + ID_FILENAME);
		SyncedFile syncedFile = new SyncedFile(currentPath + "/" + argument);


		if (idFile.exists())
		{
			readIdFromFile();
		}
		else
		{
			System.out.println("\nYou first need to get an ID from the server to be able to push a file.\n");
			return;
		}

		try
		{
			boolean ack = serverStub.push(argument, syncedFile.getContent(), clientID);

			if (ack == true)
			{
				System.out.println("\n" + argument + " has been pushed to the serveur.\n");
			}
			else
			{
				System.out.println("\n" + argument + " can't be pushed to the serveur.");
				System.out.println("Either you didn't lock it first, do not own the lock or an error has occured.\n");
			}
		}
		catch (RemoteException e)
		{
			System.out.println(e.getMessage());
			e.printStackTrace();
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

	public void parseArgs(String[] args){
		commandStr = args[1];

		if( commandStr.equals("create") ){
			command = Command.CREATE;
		} else if( commandStr.equals("list") ){
			command = Command.LIST;
		} else if (commandStr.equals("get") ){
			command = Command.GET;
		} else if (commandStr.equals("syncLocalDirectory") ){
			command = Command.SYNC;
		} else if (commandStr.equals("lock") ){
			command = Command.LOCK;
		} else if (commandStr.equals("push") ){
			command = Command.PUSH;
		} else {
			System.out.print("You need to use one of the following parameters :"
					+ "\n-\tlist\n-\tcreate\n-\tlock\n-\tget\n-\tsyncLocalDirectory\n");
			System.exit(1);
		}

		if(args.length > 2)
			argument = args[2];
		validateArgs();
	}

	private void validateArgs(){
		// Check if command takes arguments
		switch(command){
			case CREATE:
				if( argument == null ){
					System.out.println("Command " + commandStr + " requires an argument.");
					System.exit(1);
				}
				break;
			case GET:
				if( argument == null ){
					System.out.println("Command " + commandStr + " requires an argument.");
					System.exit(1);
				}
				break;
			case PUSH:
				if( argument == null ){
					System.out.println("Command " + commandStr + " requires an argument.");
					System.exit(1);
				}
				break;
			case LOCK:
				if( argument == null ){
					System.out.println("Command " + commandStr + " requires an argument.");
					System.exit(1);
				}
				break;
			case LIST:
				if( argument != null ){
					System.out.println("Command " + commandStr + " does not take any arguments.");
					System.exit(1);
				}
			case SYNC:
				if( argument != null ){
					System.out.println("Command " + commandStr + " does not take any arguments.");
					System.exit(1);
				}
				break;
		}
	}

	private void saveClientID(int clientID)
	{

	}

	private void getClientID(){
		File idFile = new File(ID_FILENAME);

		if(idFile.exists()){
			readIdFromFile();
			System.out.println("Read ID " + String.valueOf(clientID) + " from file");
		} else {
			try {
				clientID = serverStub.createClientID();
			} catch ( IOException e){
				e.printStackTrace();
			}
			System.out.println("Got ID " + String.valueOf(clientID) + "  from server");
			// write ID to file
			writeIdToFile();
		}
	}

	private void writeIdToFile()
	{
		try {
			FileWriter fw = new FileWriter(ID_FILENAME);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(String.valueOf(clientID));
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void readIdFromFile()
	{
		String idStr = null;
		try {
			FileReader fr = new FileReader(ID_FILENAME);
			BufferedReader br = new BufferedReader(fr);
			idStr = br.readLine();
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			clientID = Integer.parseInt(idStr);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}
}
