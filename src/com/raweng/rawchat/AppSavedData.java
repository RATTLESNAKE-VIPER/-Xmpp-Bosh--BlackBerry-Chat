package com.raweng.rawchat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;



public class AppSavedData {
	public static BChat bchat;

	public static boolean autoRetry = true;
	public static int retryDelay = 10;
	public static int retryLimit = 10;
	public static int fontSize = 18;

	public static void resetData() {
		autoRetry = true;
		retryDelay = 10;
		retryLimit = 10;
		fontSize = 18;

		// clean login info
		destroyUserInfo();

		//set default options
		saveOptions();

	}

	public static void saveOptions() {
		try {
			RecordStore store = RecordStore.openRecordStore("options", true);
			int numRecord = store.getNumRecords();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream os = new DataOutputStream(baos);
			os.writeBoolean(autoRetry);
			os.writeInt(retryDelay);
			os.writeInt(retryLimit);
			os.writeInt(fontSize);

			byte[] data = baos.toByteArray();

			if (numRecord == 0) {
				store.addRecord(data, 0, data.length);
			} else {
				store.setRecord(1, data, 0, data.length);
			}

			store.closeRecordStore();
		} catch (RecordStoreFullException e) {
			e.printStackTrace();
		} catch (RecordStoreNotFoundException e) {
			e.printStackTrace();
		} catch (RecordStoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void readOptions() {
		try {
			RecordStore store = RecordStore.openRecordStore("options", true);
			int numRecord = store.getNumRecords();

			if (numRecord > 0) {
				byte[] data = store.getRecord(1);

				DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));
				autoRetry = is.readBoolean();//os.writeBoolean(autoRetry);
				retryDelay = is.readInt();//os.writeInt(retryDelay);
				retryLimit = is.readInt();//os.writeInt(retryLimit);
				fontSize = is.readInt();//os.writeInt(fontSize);
			} 

			store.closeRecordStore();
		} catch (RecordStoreFullException e) {
			e.printStackTrace();
		} catch (RecordStoreNotFoundException e) {
			e.printStackTrace();
		} catch (RecordStoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void setUserInfo(String username, String password, String server, String port, boolean isBosh, String boshUrl, boolean usessl, int networkType) {
		RecordStore store = null;
		int numRecord = -1;
		try {
			store = RecordStore.openRecordStore("userinfo", true);
			numRecord = store.getNumRecords();
			// convert user info into byte array
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream os = new DataOutputStream(baos);

			os.writeBoolean(true);
			os.writeUTF(username);
			os.writeUTF(password);
			os.writeInt(networkType);
			os.writeUTF(server);
			os.writeUTF(port);
			os.writeBoolean(isBosh);
			os.writeUTF(boshUrl);
			os.writeBoolean(usessl);
			

			os.close();

			// check if the store is empty
			if (numRecord == 0) {
				byte[] data = baos.toByteArray();
				store.addRecord(data, 0, data.length);
			} else {
				byte[] data = baos.toByteArray();
				store.setRecord(1, data, 0, data.length);
			}
			store.closeRecordStore();

		} catch (RecordStoreFullException e) {
			e.printStackTrace();
		} catch (RecordStoreNotFoundException e) {
			e.printStackTrace();
		} catch (RecordStoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}



	}

	public static Vector getUserInfo() {
		RecordStore store = null;
		int numRecord = -1;
		try {
			store = RecordStore.openRecordStore("userinfo", true);
			numRecord = store.getNumRecords();

			// empty recordstore
			if (numRecord == 0)
				return null;

			byte[] data = store.getRecord(1);
			DataInputStream is = new DataInputStream(new ByteArrayInputStream(data));
			boolean saved = is.readBoolean();

			if (saved) {
				String username = is.readUTF();
				String password = is.readUTF();
				String networkType = String.valueOf(is.readInt());

				ServerModel serverDef = new ServerModel();

				Vector v = new Vector();
				v.addElement(username);
				v.addElement(password);
				v.addElement(networkType);
				serverDef.server = is.readUTF();
				serverDef.port = is.readUTF();
				serverDef.useBosh = is.readBoolean();
				serverDef.boshUrl = is.readUTF();
				serverDef.usessl = is.readBoolean();
				
				v.addElement(serverDef);

				store.closeRecordStore();
				return v;
			} else {
				store.closeRecordStore();
				return null;
			}
		} catch (RecordStoreFullException e) {
			e.printStackTrace();
		} catch (RecordStoreNotFoundException e) {
			e.printStackTrace();
		} catch (RecordStoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static void destroyUserInfo() {
		RecordStore store = null;
		int numRecord = -1;
		try {
			store = RecordStore.openRecordStore("userinfo", true);
			numRecord = store.getNumRecords();

			if (numRecord != 0) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream os = new DataOutputStream(baos);

				os.writeBoolean(false);
				os.close();
				byte[] data = baos.toByteArray();
				store.setRecord(1, data, 0, data.length);
				store.closeRecordStore();
			}
		} catch (RecordStoreFullException e) {
			e.printStackTrace();
		} catch (RecordStoreNotFoundException e) {
			e.printStackTrace();
		} catch (RecordStoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
