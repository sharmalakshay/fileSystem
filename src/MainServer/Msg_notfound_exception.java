/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer;

/**
 *
 * @author LSHARMA
 */
public class Msg_notfound_exception extends Exception {

	/**
	 * Exception thrown when the server finds missing messages in a committed transaction
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Missing messages sequence numbers
	 */
	private int[] msgNum = null; 
	public int[] getMsgNum() {
		return msgNum;
	}
	public void setMsgNum(int[] msgNum) {
		this.msgNum = msgNum;
	}
	

}
