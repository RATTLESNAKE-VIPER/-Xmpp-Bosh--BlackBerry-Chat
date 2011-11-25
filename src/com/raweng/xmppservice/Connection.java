package com.raweng.xmppservice;

import com.raweng.rawchat.ChatManager;


public class Connection {
	private static Connection instance = null;	
	private Thread thread;
	private String host, port, username, password, myjid, server;
	private String httpburl;
	private int isGoogle = 0;
	private boolean useSSL;
	private String resource;
	private String connectionMask;
	private int priority;
	private boolean isBosh;
	private int networkType;
	private ChatManager chatHandler;
	
	
	
	public static Connection getInstance() {
		if(instance==null) {
			instance = new Connection();
		}
		return instance;
	}
	

	private Connection() {
		super();
	}

	
	public void addListener(XmppListener xmppListener) {
		if (thread != null) {
			if (thread instanceof XMPPConnection) {
				((XMPPConnection) thread).addListener(xmppListener);
			} else if (thread instanceof BoshConnection) {
				((BoshConnection) thread).addListener(xmppListener);
			}
		}
	}
	
	public void removeListener(XmppListener xmppListener) {
		if (thread != null) {
			if (thread instanceof XMPPConnection) {
				((XMPPConnection) thread).removeListener(xmppListener);
			} else if (thread instanceof BoshConnection) {
				((BoshConnection) thread).removeListener(xmppListener);
			}
		}
	}
	
	
	public void connect(boolean isBosh) {
		this.isBosh = isBosh;
		if (!isBosh) {
			thread = new XMPPConnection(this);
		} else {
			thread = new BoshConnection(this);
		}
	}
	
	public ChatManager getChatHandlerInstance() {
		if (this.chatHandler == null) {
			this.chatHandler = new ChatManager(this);
		}
		return this.chatHandler;
	}
	



	public Thread getThread() {
		return thread;
	}


	public void setThread(Thread thread) {
		this.thread = thread;
	}


	public String getHost() {
		return host;
	}


	public void setHost(String host) {
		this.host = host;
	}


	public String getPort() {
		return port;
	}


	public void setPort(String port) {
		this.port = port;
	}


	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public String getMyjid() {
		return myjid;
	}


	public void setMyjid(String myjid) {
		this.myjid = myjid;
	}


	public String getServer() {
		return server;
	}


	public void setServer(String server) {
		this.server = server;
	}


	public String getHttpburl() {
		return httpburl;
	}


	public void setHttpburl(String httpburl) {
		this.httpburl = httpburl;
	}


	public int getIsGoogle() {
		return isGoogle;
	}


	public void setIsGoogle(int isGoogle) {
		this.isGoogle = isGoogle;
	}


	public boolean isSSL() {
		return useSSL;
	}


	public void setSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}


	public String getResource() {
		return resource;
	}


	public void setResource(String resource) {
		this.resource = resource;
	}


	public String getConnectionMask() {
		return connectionMask;
	}


	public void setConnectionMask(String connectionMask) {
		this.connectionMask = connectionMask;
	}


	public int getPriority() {
		return priority;
	}


	public void setPriority(int priority) {
		this.priority = priority;
	}


	public boolean isBosh() {
		return isBosh;
	}


	public void setBosh(boolean isBosh) {
		this.isBosh = isBosh;
	}


	public int getNetworkType() {
		return this.networkType;
	}


	public void setNetworkType(int networkType) {
		this.networkType = networkType;
	}
}
