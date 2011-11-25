package com.raweng.xmppservice;

import java.io.IOException;
import java.util.Vector;

public abstract class XMPPThread extends Thread {

	protected String host, port, username, password, myjid, server;
	protected String httpurl="";
	protected boolean use_ssl;
	protected String resource="Mobile";
	protected int connectionMaskIndex;
	protected int priority = 10;

	protected Connection connection;
	protected PacketParser packetParser;
	protected Vector listeners = new Vector();



	public XMPPThread(Connection connection) {
		this.connection = connection;
		this.packetParser = new PacketParser(this);
		start();
	}


	protected abstract void connect() throws Exception;
	protected abstract boolean login() throws Exception;
	protected abstract void parse() throws Exception;
	protected abstract boolean doAuthentication() throws Exception;


	protected void addListener(final XmppListener xl) {
		if(!listeners.contains(xl)) listeners.addElement(xl);
	}
	protected void removeListener(final XmppListener xl) {
		listeners.removeElement(xl);
	}





	public abstract void getRoster() throws IOException;
	public abstract void sendMessage(final String to, final String msg, final String id);	
	public abstract void subscribe(final String to);	
	public abstract void unsubscribe(final String to);	
	public abstract void subscribed(final String to);	
	public abstract void unsubscribed(final String to);	
	public abstract void setStatus(String show, String status, final int priority);	
	public abstract void sendPresence(final String to, final String type, final String show, final String status, final int priority);
	public abstract void logoff();	
	public abstract void getRosterVCard(String tojid) throws IOException;
}
