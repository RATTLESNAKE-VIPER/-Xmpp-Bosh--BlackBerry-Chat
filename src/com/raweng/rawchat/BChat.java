package com.raweng.rawchat;

import java.util.Vector;

import net.rim.device.api.ui.UiApplication;

import com.raweng.xmppservice.Connection;


public class BChat extends UiApplication {
	
	public static boolean DEBUG = true;
	public static DebugScreen debugConsole;
	private Connection connection;
	
	private LoginScreen loginscreen;
	

	public static void main(String[] args) {
		if (BChat.DEBUG) {
			BChat.debugConsole = new DebugScreen();
		}
		
		BChat app = new BChat();
		app.enterEventDispatcher();
	}
	
	public BChat() {
		ChatManager.bchat = this;
		BuddyScreen.btalk = this;
		Buddy.btalk = this;
		LoginScreen.btalk = this;
		AppSavedData.bchat = this;
		
		
		this.connection = Connection.getInstance();
		
		
		AppSavedData.readOptions();
		Vector up = AppSavedData.getUserInfo();
		if (up != null) {
			String username = "";
			String domain = "";
			if ((String)up.elementAt(0) != null && ((String)up.elementAt(0)).length() > 0) {
				int i = ((String)up.elementAt(0)).indexOf('@');
				username = ((String)up.elementAt(0)).substring(0, i);
				domain = ((String)up.elementAt(0)).substring(i+1);
			}
			String myjid = username + "@" + domain;
			
			this.connection.setUsername(username);
			this.connection.setPassword((String)up.elementAt(1));
			this.connection.setHost(domain);
			this.connection.setMyjid(myjid);
			this.connection.setNetworkType(Integer.parseInt((String) up.elementAt(2)));
			
			ServerModel serverDef = (ServerModel) up.elementAt(3);
			this.connection.setServer(serverDef.server);
			this.connection.setPort(serverDef.port);
			this.connection.setHttpburl(serverDef.boshUrl);
			this.connection.setBosh(serverDef.useBosh);
			this.connection.setSSL(serverDef.usessl);
			
			
			BuddyListField buddyList = new BuddyListField();
			BuddyScreen buddyscreen = new BuddyScreen(buddyList);
			pushScreen(buddyscreen);
			connection.getChatHandlerInstance().login(username, (String)up.elementAt(1), domain, serverDef, this.connection.getNetworkType());
			
			/*this.loginscreen = new LoginScreen(this.connection, true);
			this.pushScreen(this.loginscreen);*/
			
		} else {
			this.loginscreen = new LoginScreen(this.connection, false);
			this.pushScreen(this.loginscreen);
		}
	}
	
	
}
