/*
 * Copyright 2004-2006 Swen Kummer, Dustin Hass, Sven Jost, Grzegorz Grasza
 * modified by Yuan-Chu Tai
 * http://jxa.sourceforge.net/
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. Mobber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with mobber; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package com.raweng.xmppservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.SecureConnection;
import javax.microedition.io.StreamConnection;

import net.sourceforge.jxa.XmlReader;
import net.sourceforge.jxa.XmlWriter;

/**
 * J2ME XMPP API Class
 * 
 * @author Swen Kummer, Dustin Hass, Sven Jost, Grzegorz Grasza
 * @version 4.0
 * @since 1.0
 */

public class XMPPConnection extends XMPPThread {

	private XmlReader reader;
	private XmlWriter writer;
	private InputStream is;
	private OutputStream os;

	/**
	 * If you create this object all variables will be saved and the
	 * method {@link #run()} is started to log in on jabber server and
	 * listen to parse incomming xml stanzas. Use
	 * {@link #addListener(XmppListener xl)} to listen to events of this object.
	 */

	// jid must in the form "username@host"
	// to login Google Talk, set port to 5223 (NOT 5222 in their offical guide)	
	public XMPPConnection(Connection connection) {
		super(connection);

		this.host = connection.getHost();
		this.port = connection.getPort();
		this.username = connection.getUsername();
		this.password = connection.getPassword();
		this.resource = "mobile";
		this.myjid = this.username + "@" + this.host;
		if (connection.getServer() == null)
			this.server = host;
		else
			this.server = connection.getServer();
		this.use_ssl = connection.isSSL();
		this.connectionMaskIndex = connection.getNetworkType();
	}

	/**
	 * The <code>run</code> method is called when {@link XMPPConnection} object is
	 * created. It sets up the reader and writer, calls {@link #login()}
	 * methode and listens on the reader to parse incomming xml stanzas.
	 */
	public void run() {		
		try {
			this.connect();
		} catch (final IOException e) {
			e.printStackTrace();
			this.connectionFailed(e.getMessage());
			return;
		} catch (Exception e) {
			e.printStackTrace();
			this.connectionFailed(e.getMessage());
			return;
		}

		// connected
		try {
			boolean loginSuccess = this.login();
			if (loginSuccess) {
				this.parse();
			}			
		} catch (final Exception e) {
			// hier entsteht der connection failed bug (Network Down)
			java.lang.System.out.println(e);
			this.connectionFailed(e.toString());
		}
	}


	protected void connect() throws IOException, Exception {
		if (!use_ssl) {			
			//final StreamConnection connection = (StreamConnection) Connector.open("http://" + this.server + ":" + this.port+this.connectionMask, Connector.READ_WRITE);
			ConnectionFactory connectionFactory = new ConnectionFactory("socket://" + this.server + ":" + this.port, this.connectionMaskIndex);
			StreamConnection connection = null;

			try {
				connection = (StreamConnection) connectionFactory.getNextConnection();

			} catch (NoMoreTransportsException e) {
				throw new Exception("Connection failed. No transport available.");

			} catch (ConnectionNotFoundException e) {
				throw new Exception("ConnectionNotFoundException: " + e.getMessage());

			} catch (IllegalArgumentException e) {
				throw new Exception("IllegalArgumentException: " + e.getMessage());

			} catch (IOException e) {
				throw new Exception("IOException: " + e.getMessage());

			}

			is = connection.openInputStream();
			os = connection.openOutputStream();

			this.reader = new XmlReader(is);
			this.writer = new XmlWriter(os);

		} else {
			//final SecureConnection sc = (SecureConnection) Connector.open("ssl://" + this.server + ":" + this.port+this.connectionMask, Connector.READ_WRITE);
			ConnectionFactory connectionFactory = new ConnectionFactory("ssl://" + this.server + ":" + this.port, this.connectionMaskIndex);
			SecureConnection sc = null;
			try {
				sc = (SecureConnection) connectionFactory.getNextConnection();

			} catch (NoMoreTransportsException e) {
				throw new Exception("Connection failed. No transport available.");

			} catch (ConnectionNotFoundException e) {
				throw new Exception("ConnectionNotFoundException: " + e.getMessage());

			} catch (IllegalArgumentException e) {
				throw new Exception("IllegalArgumentException: " + e.getMessage());

			} catch (IOException e) {
				throw new Exception("IOException: " + e.getMessage());

			}

			if (sc != null) {
				//sc.setSocketOption(SocketConnection.DELAY, 1);
				//sc.setSocketOption(SocketConnection.LINGER, 0);
				is = sc.openInputStream();
				os = sc.openOutputStream();
				this.reader = new XmlReader(is);
				this.writer = new XmlWriter(os);
			}

		}
	}

	/**
	 * Opens the connection with a stream-tag, queries authentication type and
	 * sends authentication data, which is username, password and resource.
	 * @return 
	 * @throws Exception 
	 */
	protected boolean login() throws Exception {
		String msg = "<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' to='" + this.host + "' version='1.0'>";
		os.write(msg.getBytes());
		os.flush();	
		do {
			reader.next();
			if (reader.getType() == XmlReader.START_TAG && reader.getName().equals("stream:features")) {
				this.packetParser.parseFeatures(reader);
			}	            
		} while (!(reader.getType() == XmlReader.END_TAG && reader.getName().equals("stream:features")));


		boolean loginSuccess = this.doAuthentication();


		msg = "<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' to='" + this.host + "' version='1.0'>";
		os.write(msg.getBytes());
		os.flush();
		reader.next();
		while (true) {
			if ((reader.getType() == XmlReader.END_TAG) && reader.getName().equals("stream:features")) {
				break;
			}
			reader.next();
		}


		if (resource == null) {
			msg = "<iq type='set' id='res_binding'><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'/></iq>";
		} else {
			msg = "<iq type='set' id='res_binding'><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'><resource>" + resource + "</resource></bind></iq>";
		}
		os.write(msg.getBytes());
		os.flush();

		return loginSuccess;
	}


	/**
	 * The main parse methode is parsing all types of XML stanzas
	 * <code>message</code>, <code>presence</code> and <code>iq</code>.
	 * Although ignores any other type of xml.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	protected void parse() throws IOException {
		while (true) {
			int nextTag = this.reader.next();
			switch (nextTag) {
			case XmlReader.START_TAG:
				final String tmp = this.reader.getName();
				if (tmp.equals("message")) {
					this.packetParser.parseMessage(this.reader);
				} else if (tmp.equals("presence")) {
					this.packetParser.parsePresence(this.reader);
				} else if (tmp.equals("iq")) {
					this.packetParser.parseIq(this.reader, this.writer);
				} else {
					this.packetParser.parseIgnore(this.reader);
				}
				break;

			case XmlReader.END_TAG:
				this.reader.close();
				throw new IOException("Unexpected END_TAG "+this.reader.getName());

			default:
				this.reader.close();
				throw new IOException("Bad XML tag");
			}
		}
	}


	protected boolean doAuthentication() throws Exception {
		boolean loginSuccess = false;

		Vector mechanismList = this.packetParser.getMechanism();
		if (mechanismList.contains("X-GOOGLE-TOKEN")) {
			// X-GOOGLE-TOKEN authorization doing. User can disable
			// google features using by deselecting corresponding
			// checkbox in profile
			String resp = this.packetParser.getGoogleToken(this.myjid, this.password);
			
			String msg = "<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"X-GOOGLE-TOKEN\">" + resp + "</auth>";
			os.write(msg.getBytes());
			os.flush();	    
			
			reader.next();
			if (reader.getName().equals("success")) {
				loginSuccess = true;
				while (true) {
					if ((reader.getType() == XmlReader.END_TAG) && reader.getName().equals("success")) {
						break;
					}
					reader.next();
				}
			}
		}		


		if (mechanismList.contains("PLAIN") && loginSuccess == false) {
			String msg = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>";
			byte[] auth_msg = (username + "@" + host + "\0" + username + "\0" + password).getBytes();
			msg = msg + MD5.toBase64(auth_msg) + "</auth>";
			os.write(msg.getBytes());
			os.flush();
			reader.next();
			if (reader.getName().equals("success")) {
				loginSuccess = true;
				while (true) {
					if ((reader.getType() == XmlReader.END_TAG) && reader.getName().equals("success")) {
						break;
					}
					reader.next();
				}
			}
		}

		if (loginSuccess == false) {
			for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
				XmppListener xl = (XmppListener) e.nextElement();
				xl.onAuthFailed(reader.getName() + ", failed authentication");
			}
			return false;
		}

		return loginSuccess;
	}














	public void getRosterVCard(String tojid) throws IOException {
		this.writer.startTag("iq");
		this.writer.attribute("id", "vc2");
		this.writer.attribute("to", tojid);
		this.writer.attribute("type", "get");
		this.writer.startTag("vCard");
		this.writer.attribute("xmlns", "vcard-temp");
		this.writer.endTag(); // vCard
		this.writer.endTag(); // iq
		this.writer.flush();
	}


	/**
	 * Sends a roster query.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	public void getRoster() throws IOException {
		this.writer.startTag("iq");
		this.writer.attribute("id", "roster");
		this.writer.attribute("type", "get");
		this.writer.startTag("query");
		this.writer.attribute("xmlns", "jabber:iq:roster");
		this.writer.endTag(); // query
		this.writer.endTag(); // iq
		this.writer.flush();
	}	

	/**
	 * Sends a message text to a known jid.
	 * 
	 * @param to the JID of the recipient
	 * @param msg the message itself
	 */
	public void sendMessage(final String to, final String msg, final String id) {
		try {
			this.writer.startTag("message");
			this.writer.attribute("type", "chat");
			this.writer.attribute("to", to);
			this.writer.startTag("body");
			this.writer.text(msg);
			this.writer.endTag();
			this.writer.endTag();
			this.writer.flush();
		} catch (final IOException e) {
			java.lang.System.out.println(e);
			this.connectionFailed();
		}
	}

	/**
	 * Requesting a subscription.
	 * 
	 * @param to the jid you want to subscribe
	 */
	public void subscribe(final String to) {
		this.sendPresence(to, "subscribe", null, null, 0);
	}

	/**
	 * Remove a subscription.
	 * 
	 * @param to the jid you want to remove your subscription
	 */
	public void unsubscribe(final String to) {
		this.sendPresence(to, "unsubscribe", null, null, 0);
	}

	/**
	 * Approve a subscription request.
	 * 
	 * @param to the jid that sent you a subscription request
	 */
	public void subscribed(final String to) {
		this.sendPresence(to, "subscribed", null, null, 0);
	}

	/**
	 * Refuse/Reject a subscription request.
	 * 
	 * @param to the jid that sent you a subscription request
	 */
	public void unsubscribed(final String to) {
		this.sendPresence(to, "unsubscribed", null, null, 0);
	}	

	/**
	 * Sets your Jabber Status.
	 * 
	 * @param show is one of the following: <code>null</code>, chat, away,
	 *        dnd, xa, invisible
	 * @param status an extended text describing the actual status
	 * @param priority the priority number (5 should be default)
	 */
	public void setStatus(String show, String status, final int priority) {
		if (show.equals("")) {
			show = null;
		}
		if (status.equals("")) {
			status = null;
		}
		if (show.equals("invisible")) {
			this.sendPresence(null, "invisible", null, null, priority);
		} else {
			this.sendPresence(null, null, show, status, priority);
		}
	}

	/**
	 * Sends a presence stanza to a jid. This method can do various task but
	 * it's private, please use setStatus to set your status or explicit
	 * subscription methods subscribe, unsubscribe, subscribed and
	 * unsubscribed to change subscriptions.
	 */
	public void sendPresence(final String to, final String type, final String show, final String status, final int priority) {
		try {
			this.writer.startTag("presence");
			if (type != null) {
				this.writer.attribute("type", type);
			}
			if (to != null) {
				this.writer.attribute("to", to);
			}
			if (show != null) {
				this.writer.startTag("show");
				this.writer.text(show);
				this.writer.endTag();
			}
			if (status != null) {
				this.writer.startTag("status");
				this.writer.text(status);
				this.writer.endTag();
			}
			if (priority != 0) {
				this.writer.startTag("priority");
				this.writer.text(Integer.toString(priority));
				this.writer.endTag();
			}
			this.writer.endTag(); // presence
			this.writer.flush();
		} catch (final IOException e) {
			java.lang.System.out.println(e);
			this.connectionFailed();
		}
	}

	/**
	 * Closes the stream-tag and the {@link XmlWriter}.
	 */
	public void logoff() {
		try {
			this.writer.endTag();
			this.writer.flush();
			this.writer.close();
		} catch (final IOException e) {
			java.lang.System.out.println(e);
			this.connectionFailed();
		}
	}







	/**
	 * Save a contact to roster. This means, a message is send to jabber
	 * server (which hosts your roster) to update the roster.
	 * 
	 * @param jid the jid of the contact
	 * @param name the nickname of the contact
	 * @param group the group of the contact
	 * @param subscription the subscription of the contact
	 */
	public void saveContact(final String jid, final String name, final Enumeration group, final String subscription) {
		try {
			this.writer.startTag("iq");
			this.writer.attribute("type", "set");
			this.writer.startTag("query");
			this.writer.attribute("xmlns", "jabber:iq:roster");
			this.writer.startTag("item");
			this.writer.attribute("jid", jid);
			if (name != null) {
				this.writer.attribute("name", name);
			}
			if (subscription != null) {
				this.writer.attribute("subscription", subscription);
			}
			if (group != null) {
				while (group.hasMoreElements()) {
					this.writer.startTag("group");
					this.writer.text((String) group.nextElement());
					this.writer.endTag(); // group
				}
			}
			this.writer.endTag(); // item
			this.writer.endTag(); // query
			this.writer.endTag(); // iq
			this.writer.flush();
		} catch (final IOException e) {
			java.lang.System.out.println(e);
			this.connectionFailed();
		}
	}

















	/**
	 * This method is used to be called on a parser or a connection error.
	 * It tries to close the XML-Reader and XML-Writer one last time.
	 *
	 */
	private void connectionFailed() {
		if (this.writer != null)
			this.writer.close();

		if (this.reader != null)
			this.reader.close();

		for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
			XmppListener xl = (XmppListener) e.nextElement();
			xl.onConnFailed("");
		}
	}

	private void connectionFailed(final String msg) {
		if (this.writer != null)
			this.writer.close();

		if (this.reader != null)
			this.reader.close();

		for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
			XmppListener xl = (XmppListener) e.nextElement();
			xl.onConnFailed(msg);
		}
	}

};
