/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer;


import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;

/**
 *
 * @author LSHARMA
 */
public class MainServer implements Server_interface, Connection_reply {
	// default directories
	String dir = System.getProperty("user.home") + "/dfs/";
	String logfile = dir + "log/";
	private ArrayList<ReplicaServerInfo> replicaservers = new ArrayList<MainServer.ReplicaServerInfo>();

	/**
	 * Hashtable of all transaction
	 * */
	private Hashtable<Long, Transaction> transactions = new Hashtable<Long, Transaction>();

	/**
	 * Hashtable of all clients
	 * */
	private Hashtable<String, Client_interface> clients = new Hashtable<String, Client_interface>();

//	private Set<String> fileLocks = Collections.synchronizedSet(new HashSet<String>());
	private HashSet<String> fileLocks = new HashSet<String>();
	
	/**
	 * log instance to log clients interaction with the server
	 * */
	private log logs;
	private Random rand;
	private static long maxtimeout = 100000; 

	// secondary server attributes
	Server2 server2;

	public static final String main_connection_name = "main_server_responder";

	/**
	 * Constructing MainServe object with main attributes, this is used when
	 * running new MainServer instance from the secondary server when the
	 * original main server is failed.
	 * */
	public MainServer(log logs,
			Hashtable<String, Client_interface> clients,
			Hashtable<Long, Transaction> transactions, String directoryPath) {

		this.logs = logs;
		this.dir = directoryPath;
		this.clients = clients;
		this.transactions = transactions;
		this.rand = new Random(System.currentTimeMillis());
	}

	public MainServer(String secondaryServerHost, int secondaryServerPort, String directoryPath)
			throws RemoteException, NotBoundException {
		if (directoryPath != null) {
			this.dir = directoryPath;
			this.logfile = dir + "log/";
		}
		
		this.clients = new Hashtable<String, Client_interface>();
		this.rand = new Random(System.currentTimeMillis());
		
		// getting access to the secondary server if it is given as paramter
		if (secondaryServerHost != null) {
			Registry registry = LocateRegistry.getRegistry(secondaryServerHost, secondaryServerPort);
			server2 = (Server2) registry.lookup(DFS_SECONDARY_SERVER_UNIQUE_NAME);
		}

		// creating working directories
		new File(logfile).mkdir();

		// create logs
		this.logs = new log();
		this.logs.init(logfile + "log.txt");
	}

	Second_server getServer(ReplicaServerInfo replicaServerInfo) {
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(replicaServerInfo.hostName, replicaServerInfo.port);
			return (Second_server) registry.lookup(replicaServerInfo.uniqueName);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	
	public File_contents read(String fileName) throws FileNotFoundException,
			IOException, RemoteException {
		
		int idx = rand.nextInt(replicaservers.size());
		
		Server_interface rServer = (Server_interface) getServer(replicaservers.get(idx));
		File_contents contents = rServer.read(fileName);
		
		long time = System.currentTimeMillis();
		logs.logReadFile(fileName, time);

		if (server2 != null)
			server2.read(fileName, time);

		// return FileContent instance
		return contents;
	}

	
	public long newTxn(String fileName) throws RemoteException, IOException {
		// generate new transaction id
		long time = System.currentTimeMillis();
		long txnId = time;
		// create transaction object and log it
		Transaction tx = new Transaction(fileName, Transaction.STARTED, txnId, time);
		
		logs.logTransaction(tx, time);
		transactions.put(txnId, tx);
		
		if (server2 != null)
			server2.newTxn(fileName, txnId, time);
		
		return txnId;
	}

	
	public int write(long txnID, long msgSeqNum, byte[] data)
			throws RemoteException, IOException {
		// check if the transaction id is correct
		if (!transactions.containsKey(txnID)) {
			return INVALID_TRANSACTION_ID;
		}

		// check if the transaction has been already committed
		if (transactions.get(txnID).getState() == Transaction.COMMITED) {
			return INVALID_OPERATION;
		}
		
		for (ReplicaServerInfo name : replicaservers) {
			Second_server server = (Second_server) getServer(name);
			
			if (server != null)
				server.write(txnID, msgSeqNum, data);
		}
		
		// log this write request
		long time = System.currentTimeMillis();
		logs.logWriteRequest(txnID, msgSeqNum, data.length, time);
		
		if (server2 != null)
			server2.write(txnID, msgSeqNum, data.length, time);
		
		return ACK;
	}

	
	public int commit(final long txnID, long numOfMsgs)
			throws Msg_notfound_exception, RemoteException {
		// check if the transaction id is correct
		if (!transactions.containsKey(txnID)) {
			return INVALID_TRANSACTION_ID;
		}
		// check if the transaction has been already committed
		if (transactions.get(txnID).getState() == Transaction.COMMITED) {
			// the client me request resending the ack message
			return ACK;
		}
		
		Transaction tx = transactions.get(txnID);
		
		// granting lock on the file name
		grantFileLock(tx.getFileName());
		
		for (ReplicaServerInfo name : replicaservers) {
			Second_server server = (Second_server) getServer(name);
			
			if (server != null)
				server.commit(txnID, numOfMsgs, tx.getFileName());
		}
		
		// update transaction state and log it
		tx.setState(Transaction.COMMITED);
		long time = System.currentTimeMillis();
		logs.logTransaction(tx, time);
		
		// release file lock
		releaseFileLock(tx.getFileName());
		
		if (server2 != null)
			server2.commit(txnID, tx.getFileName(), time);
		
		return ACK;
	}

	
	public int abort(long txnID) throws RemoteException {
		// check if the transaction id is correct
		if (!transactions.containsKey(txnID)) {
			return INVALID_TRANSACTION_ID;
		}

		// check if the transaction has been already committed
		if (transactions.get(txnID).getState() == Transaction.COMMITED) {
			// aborting commited transaction is invalid operation
			return INVALID_OPERATION;
		}

		// check if the transaction has been already aborted
		if (transactions.get(txnID).getState() == Transaction.ABORTED) {
			return ACK;
		}

		for (ReplicaServerInfo name : replicaservers) {
			Second_server server = (Second_server) getServer(name);
			
			if (server != null)
				server.abort(txnID);
		}
		
		// update transaction state and log it
		long time = System.currentTimeMillis();
		transactions.get(txnID).setState(Transaction.ABORTED);
		logs.logTransaction(transactions.get(txnID), time);

		if (server2 != null)
			server2.abort(txnID, transactions.get(txnID).getFileName(), time);

		return ACK;
	}

	
	public boolean registerClient(Client_interface client)
			throws RemoteException {
		String auth_token = client.getAuthenticationToken();

		if (auth_token == null) {
			// generate new auth token
			auth_token = UUID.randomUUID().toString();
			client.setAuthenticationToken(auth_token);

			// add this new client to the list of authenticated clients
			this.clients.put(auth_token, client);

			if (server2 != null)
				server2.registerClient(client, auth_token);

			return true;
		} else {
			if (clients.containsKey(auth_token))
				return true;
			else
				return false;
		}
	}

	
	public boolean unregisterClient(Client_interface client)
			throws RemoteException {
		String auth_token = client.getAuthenticationToken();
		
		if (auth_token == null) {
			// Unresisted client
			return false;
		} else {
			if (clients.containsKey(auth_token)) {
				// safely remove this client
				clients.remove(auth_token);

				if (server2 != null)
					server2.unregisterClient(client, auth_token);

				return true;
			} else {
				// unrecognized auth token, safely return false
				return false;
			}
		}
	}

	
	public boolean isAlive() throws RemoteException {
		return true;
	}

	public void init(int port) throws java.rmi.AlreadyBoundException, IOException {
		Object mainServerExportedObject = UnicastRemoteObject.exportObject(this, port);
		Server_interface serverStub = (Server_interface) mainServerExportedObject;
		Connection_reply heartbeatResponderStub = (Connection_reply) mainServerExportedObject;

		Registry registry = LocateRegistry.createRegistry(port);
		registry.rebind(DFSERVER_UNIQUE_NAME, serverStub);
		registry.rebind(main_connection_name, heartbeatResponderStub);
		
		// running transaction time out checker thread
		transactionsTimeoutChecker.start();
		
		// read ReplicaServer configuration file
		InputStreamReader converter = new InputStreamReader(new FileInputStream(new File("ReplicaServers")));
		BufferedReader in = new BufferedReader(converter);
		String line = null;
		
		in.readLine(); // skip first line, it is a comment for specifying the format
		while ((line = in.readLine()) != null) {
			replicaservers.add(new ReplicaServerInfo(line));
		}
	}
	
	private synchronized void grantFileLock(String fileName){
		System.out.println("trying to grant lock");
		while(fileLocks.contains(fileName)){
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		fileLocks.add(fileName);
		System.out.println("lock granted!");
	}
	
	private void releaseFileLock(String fileName){
		fileLocks.remove(fileName);
		System.out.println("lock released!");
	}
	
	
	private Thread transactionsTimeoutChecker = new Thread(new Runnable() {
		
		
		public void run() {
			while(true){
				long now = System.currentTimeMillis();
				Object[] keys = MainServer.this.transactions.keySet().toArray();
				for (Object key : keys) {
					Transaction t = MainServer.this.transactions.get((Long)key);
					
					// clean aborted and commited transactions from transaction hash table
					if(t.getState() == Transaction.COMMITED || t.getState() == Transaction.ABORTED){
						MainServer.this.transactions.remove(key);
					}
					
					// check transaction time and state
					if((now - t.getLastEdited()) > maxtimeout){
						try {
							MainServer.this.abort((Long)key);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						MainServer.this.transactions.remove(key);
					}
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});
	
	class ReplicaServerInfo{
		String uniqueName;
		String hostName;
		int port;
		
		public ReplicaServerInfo(String info){
			StringTokenizer st = new StringTokenizer(info, "\t");
			hostName = st.nextToken();
			port = Integer.parseInt(st.nextToken());
			uniqueName = st.nextToken();
		}
	}
	
	public static void main(String[] args) throws AlreadyBoundException, NotBoundException,
			java.rmi.AlreadyBoundException, IOException {
		
		final MainServer server = new MainServer("localhost", 4135, System.getProperty("user.home") + "/dfs/dfs2/");
		server.init(5555);

		System.out.println("server is running ...");
	}

}