/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer.MainClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

import MainServer.Msg_notfound_exception;
import MainServer.Client_interface;
import MainServer.Server_interface;
/**
 *
 * @author LSHARMA
 */
public class Client implements Client_interface {
	private String auth_token;
	private String hostIP;
	Server_interface server;
	int port;

	public Client(String serverHostIP, int port) throws RemoteException,
			NotBoundException {
		this.hostIP = serverHostIP;
		this.port = port;

		Registry reg = LocateRegistry.getRegistry(hostIP, port);
		server = (Server_interface) reg
				.lookup(Server_interface.DFSERVER_UNIQUE_NAME);

		UnicastRemoteObject.exportObject(this, 5412);
		server.registerClient(this);
	}

	@Override
	public void updateServerIP(String ip, int port) throws RemoteException {
		this.hostIP = ip;
		this.port = port;

		try {
			Registry reg = LocateRegistry.getRegistry(hostIP, port);
			server = (Server_interface) reg
					.lookup(Server_interface.DFSERVER_UNIQUE_NAME);
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setAuthenticationToken(String auth_token)
			throws RemoteException {
		this.auth_token = auth_token;
	}

	@Override
	public String getAuthenticationToken() throws RemoteException {
		return this.auth_token;
	}

	public String read(String fileName) throws FileNotFoundException,
			RemoteException, IOException {
		byte[] content = server.read(fileName).get();
		return new String(content);
	}

	public long write(String fileName, String content)
			throws RemoteException, IOException, Msg_notfound_exception {
		long txid = server.newTxn(fileName);
		server.write(txid, 1, content.getBytes());
		return txid;
	}

	public static void main(String[] args) throws NotBoundException,
			FileNotFoundException, IOException, Msg_notfound_exception, InterruptedException {
		Client c = new Client("localhost", 5555); // 5892
		long txid = c.server.newTxn("mashary.txt");
		
		c.server.write(txid, 1, "This is a test1\n".getBytes());
		c.server.write(txid, 2, "This is a test2\n".getBytes());
		
		Thread.sleep(10000);
		
		c.server.write(txid, 3, "This is a test3\n".getBytes());
		c.server.write(txid, 4, "This is a test4\n".getBytes());
		
		try {
			c.server.commit(txid, 4);
		} catch (Msg_notfound_exception e) {
			System.out.println(Arrays.toString(e.getMsgNum()));
		}
		
		System.out.println(new String(c.server.read("mashary.txt").get()));
	}
}