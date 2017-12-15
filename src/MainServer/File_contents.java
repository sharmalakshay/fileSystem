/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer;

import java.io.*;

/**
 *
 * @author LSHARMA
 */
public class File_contents implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private byte[] contents; // file contents
	
	public static final int BUFFER_SIZE = 1024*4; // 4 KByes   
	
	public File_contents(byte[] contents) {
		this.contents = contents;
	}

	public void print() throws IOException {
		System.out.println("FileContents = " + contents);
	}

	public byte[] get() {
		return contents;
	}
}