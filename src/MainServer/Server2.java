/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer;

import java.rmi.*;
/**
 *
 * @author LSHARMA
 */
public interface Server2 extends Remote{
	public void read(String fileName, long time) throws RemoteException;

	public void newTxn(String fileName, long txnId, long time) throws RemoteException;

	public void write(long txnID, long msgSeqNum, int dataLength, long time)
			throws RemoteException;

	public void commit(long txnID, String fileName, long time)
			throws RemoteException;

	public void abort(long txnID, String fileName, long time) throws RemoteException;

	public void registerClient(Client_interface client, String auth_token)
			throws RemoteException;

	public void unregisterClient(Client_interface client, String auth_token)
			throws RemoteException;
}
