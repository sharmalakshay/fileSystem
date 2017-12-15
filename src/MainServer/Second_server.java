/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer;

import java.io.*;
import java.rmi.RemoteException;
/**
 *
 * @author LSHARMA
 */
public interface Second_server extends ServerInterface{

	FileContents read(String fileName) throws FileNotFoundException, IOException, RemoteException;

	long newTxn(String fileName) throws RemoteException, IOException;

	int write(long txnID, long msgSeqNum, byte[] data) throws RemoteException, IOException;

	int commit(long txnID, long numOfMsgs, String filename)	throws MessageNotFoundException, RemoteException;

	int abort(long txnID) throws RemoteException;

	boolean registerClient(Client_interface client) throws RemoteException;

	boolean unregisterClient(Client_interface client) throws RemoteException;

	int commit(long txnID, long numOfMsgs) throws MessageNotFoundException,	RemoteException;

}