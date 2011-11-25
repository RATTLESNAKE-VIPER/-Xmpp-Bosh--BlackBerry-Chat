package com.raweng.xmppservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.microedition.rms.RecordComparator;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordFilter;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

/**
 * A class used for storing and retrieving chat history.
 */
public class ChatHistory implements RecordFilter, RecordComparator {

	// The RecordStore used for storing the chat history.
	private RecordStore recordStore = null;

	// jid to use when filtering.
	public static String jid1Filter = null;
	public static String jid2Filter = null;





	/**
	 * The constructor opens the underlying record store,
	 * creating it if necessary.
	 */
	public ChatHistory() 
	{
		try {
			recordStore = RecordStore.openRecordStore("ChatHistory", true);
		}
		catch (RecordStoreException rse) {
			System.out.println(rse);
			rse.printStackTrace();
		}
	}



	/**
	 * Add a new chat to the storage.
	 */
	public void addToChatHistory(long time, String fromjid, String tojid, String name, String message) 
	{    	
		// Each chat is stored in a separate record.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream outputStream = new DataOutputStream(baos);
		try {
			// Push the data into a byte array.
			outputStream.writeLong(time);
			outputStream.writeUTF(fromjid);
			outputStream.writeUTF(tojid);
			outputStream.writeUTF(name);
			outputStream.writeUTF(message);
		}
		catch (IOException ioe) {
			System.out.println(ioe);
			ioe.printStackTrace();
		}

		// Extract the byte array
		byte[] b = baos.toByteArray();
		// Add it to the record store
		try {
			recordStore.addRecord(b, 0, b.length);
		}
		catch (RecordStoreException rse) {
			System.out.println(rse);
			rse.printStackTrace();
		}
	}



	public RecordEnumeration getChatHistory(String jid1, String jid2)
	{
		jid1Filter = jid1;
		jid2Filter = jid2;
		try {
			// Enumerate the records using the comparator and filter
			// implemented above to sort by chat time.
			RecordEnumeration re = recordStore.enumerateRecords(this, this, true);
			return re;
		}
		catch (RecordStoreException rse) {
			System.out.println(rse);
			rse.printStackTrace();
		}
		return null;
	}








	/*
	 * Part of the RecordFilter interface.
	 */
	public boolean matches(byte[] candidate) throws IllegalArgumentException
	{
		// If no filter set, nothing can match it.
		if (ChatHistory.jid1Filter == null && ChatHistory.jid2Filter == null) {
			return false;
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(candidate);
		DataInputStream inputStream = new DataInputStream(bais);
		String fromjid = null;
		String tojid = null;

		try {
			long time = inputStream.readLong();
			fromjid = inputStream.readUTF();
			tojid = inputStream.readUTF();
			//String name = inputStream.readUTF();
			//String message = inputStream.readUTF();
		}
		catch (EOFException eofe) {
			System.out.println(eofe);
			eofe.printStackTrace();
		}
		catch (IOException eofe) {
			System.out.println(eofe);
			eofe.printStackTrace();
		}
		boolean result = (ChatHistory.jid1Filter.equals(fromjid) && ChatHistory.jid2Filter.equals(tojid)) || 
		(ChatHistory.jid1Filter.equals(tojid) && ChatHistory.jid2Filter.equals(fromjid));

		return (result);
	}

	/*
	 * Part of the RecordComparator interface.
	 */
	public int compare(byte[] rec1, byte[] rec2)
	{
		// Construct DataInputStreams for extracting the chat times from the records.
		ByteArrayInputStream bais1 = new ByteArrayInputStream(rec1);
		DataInputStream inputStream1 = new DataInputStream(bais1);
		ByteArrayInputStream bais2 = new ByteArrayInputStream(rec2);
		DataInputStream inputStream2 = new DataInputStream(bais2);
		Calendar cal1 = null;
		Calendar cal2 = null;
		try {
			// Extract the time.
			long d1 = inputStream1.readLong();
			//String fromjid1 = inputStream1.readUTF();
			//String tojid1 = inputStream1.readUTF();
			//String name1 = inputStream1.readUTF();
			//String message1 = inputStream1.readUTF();
			
			long d2 = inputStream2.readLong();
			//String fromjid2 = inputStream2.readUTF();
			//String tojid2 = inputStream2.readUTF();
			//String name2 = inputStream2.readUTF();
			//String message2 = inputStream2.readUTF();
			
			
			Date date1 = new Date(d1);
			cal1 = Calendar.getInstance();
			cal1.setTime(date1);
			
			Date date2 = new Date(d2);
			cal2 = Calendar.getInstance();
			cal2.setTime(date2);
		}
		catch (EOFException eofe) {
			System.out.println(eofe);
			eofe.printStackTrace();
		}
		catch (IOException eofe) {
			System.out.println(eofe);
			eofe.printStackTrace();
		}
		
		if (cal1 != null && cal2 != null) {
			if(cal1.before(cal2)) {
				return RecordComparator.PRECEDES;
			}
			else if(cal1.after(cal2)) {
				return RecordComparator.FOLLOWS;
			}
			else {
				return RecordComparator.EQUIVALENT;
			}
		}
		return 0;		
	}



	public void deleteChatHistory() {
		try {
			recordStore.closeRecordStore();
			RecordStore.deleteRecordStore("ChatHistory");
		} catch (final RecordStoreNotFoundException e) {
			e.printStackTrace();
		} catch (final RecordStoreException e) {
			e.printStackTrace();
		}
	}


	public RecordStore getRecordStore() {
		return recordStore;
	}


}
