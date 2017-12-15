/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package MainServer;

import java.io.*;
import java.util.*;

/**
 *
 * @author LSHARMA
 */
public class log {
	public static final String TRANSACTION_LOG_ENTRY = "TRXN";
	public static final String READ_LOG_ENTRY = "READ";
	public static final String WRITE_LOG_ENTRY = "WMSG";
	
	protected String logFilePath;
	protected BufferedWriter log;
	
	/**
	 * Initilaizes the logger and opening stream on log file or create it
	 * */
	public void init(String logFilePath){
		this.logFilePath = logFilePath;
		try {
			File logFile = new File(logFilePath);
			logFile.createNewFile();
			log = new BufferedWriter(new FileWriter(logFile, true));
		} catch (IOException e) {
			System.err.println("unable to open log file");
			e.printStackTrace();
			
			// system can not start without log file
			System.exit(1);
		}
	}
	
	/**
	 * printing transaction information to the log
	 * 
	 * @param	tx	transaction to be logged
	 * */
	public String logTransaction(Transaction tx, long time){
		String msg = String.format("%d:%d:%s", tx.getId(), tx.getState(), tx.getFileName());
		return writeLogEntry(TRANSACTION_LOG_ENTRY, msg, time);
	}
	
	/**
	 * printing log entry when file is read
	 * 
	 * @param fileName	name of the file that is being read 
	 * */
	public String logReadFile(String fileName, long time){
		return writeLogEntry(READ_LOG_ENTRY, fileName, time);
	}
	
	
	/**
	 * printing a log entry for a transaction message
	 * 
	 * @param	txnid	id transaction
	 * @param	msgid	id of the message that has been writen
	 * @param	msgSize	size of the message that has been writen
	 * */
	public String logWriteRequest(long txnid, long msgid, long msgSize, long time){
		String msg = String.format("%d:%d:%d", txnid, msgid, msgSize);
		return writeLogEntry(WRITE_LOG_ENTRY, msg, time);
	}

	protected String writeLogEntry(String entryType, String msg, long time){
		try {
			StringBuilder stb = new StringBuilder();
			stb.append(String.format("%s:%d", entryType, time));
			stb.append('\t');
			stb.append(msg);
			stb.append('\n');
			
			log.write(stb.toString());
			log.flush();
			
			return stb.toString();
		} catch (IOException e) {
			e.printStackTrace();

			// system can not live without logging
			System.exit(1);
		}
		return null;
	}
	
	public void closeWriterStream(){
		try {
			log.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void reInit(){
		init(logFilePath);
	}
	
	public LogIterator readLog() throws FileNotFoundException{
		InputStreamReader converter = new InputStreamReader(new FileInputStream(new File(logFilePath)));
		BufferedReader in = new BufferedReader(converter);
		return new LogIterator(in);
	}
	
	class LogEntry{
		String type;
		long timeStamp;
		String fileName;
		long transactionID;
		long transactionState;
		long messageID;
		long messageSize;
		
		public LogEntry(String line){
			String type_and_timeStamp = line.substring(0, line.indexOf('\t'));
			String entryDate = line.substring(line.indexOf('\t') + 1);
			
			StringTokenizer st = new StringTokenizer(type_and_timeStamp, ":");
			this.type = st.nextToken();
			this.timeStamp = Long.parseLong(st.nextToken());
			
			if(type.equals(READ_LOG_ENTRY)){
				
				this.fileName = entryDate;

			}else if(type.equals(TRANSACTION_LOG_ENTRY)){
			
				st = new StringTokenizer(entryDate, ":");
				this.transactionID = Long.parseLong(st.nextToken());
				this.transactionState = Long.parseLong(st.nextToken());
				this.fileName = entryDate.substring(entryDate.lastIndexOf(':') + 1);
			
			}else if(type.equals(WRITE_LOG_ENTRY)){
				
				st = new StringTokenizer(entryDate, ":");
				this.transactionID = Long.parseLong(st.nextToken());
				this.messageID = Long.parseLong(st.nextToken());
				this.messageSize = Long.parseLong(st.nextToken());
			}
		}
	}

	class LogIterator implements Iterator<LogEntry>{
		BufferedReader in;
		String line = "";
		
		boolean nextElementHasbeenRead = false;
		public LogIterator(BufferedReader in) {
			this.in = in;
		}
		
		@Override
		public boolean hasNext() {
			if(nextElementHasbeenRead)
				return line == null;

			try {
				line = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			nextElementHasbeenRead = true;
			return line == null;
		}

		@Override
		public LogEntry next() {
			if(!nextElementHasbeenRead){
				try {
					line = in.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			nextElementHasbeenRead = false;
			
			if(line == null)
				return null;
			else
				return new LogEntry(line);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
}
