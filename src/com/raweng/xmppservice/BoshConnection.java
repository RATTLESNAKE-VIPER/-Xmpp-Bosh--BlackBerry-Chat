package com.raweng.xmppservice;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.HttpConnection;

import net.sourceforge.jxa.XmlReader;
import net.sourceforge.jxa.XmlWriter;

/**
 * class for transmitting stanzas over a httpconnection as definend in JEP 124
 */
public class BoshConnection extends XMPPThread {

	private static int DEFAULT_WAIT = 30;
	private int wait = DEFAULT_WAIT; // max seconds to keep the connection

	// open, default value
	private HttpConnection[] conn = new HttpConnection[2]; //allow exactly 2 connections 
	private int defaultConn = 0;
	private long rid = -1; // request id
	private String sid = null; // session id
	private boolean terminating = false; //indicates that we want to gracefully terminate
	private Thread secondThread = null;


	protected boolean ended = false; // Network error flag
	protected boolean busy = false; // Indicates if someone reads packet
	protected boolean google = false; // Google Talk server flag
	protected boolean terminated = false; // Indicates if someone closed




	public BoshConnection(Connection connection) {
		super(connection);

		this.host = connection.getHost();
		this.port = connection.getPort();
		this.username = connection.getUsername();
		this.password = connection.getPassword();
		this.resource = "mobile";
		this.myjid = this.username + "@" + this.host;
		this.httpurl = connection.getHttpburl();
		this.connectionMaskIndex = connection.getNetworkType();
	}




	public void run() {
		try {
			this.connect();
			boolean loginSuccess = this.login();
			if (loginSuccess) {
				//switching the default connection causes all new calls to
				//writeToAir to be executed in a new thread with the 2nd httpconnection object
				defaultConn = 1;
				this.parse();
			}

		} catch (Exception ex) {
			java.lang.System.out.println(ex);
			terminate();
		}
	}


	protected void connect() throws Exception {
		/** Initiate Stream (Connect to server) */
		rid = XMPPUtils.generateInitialRequestId(); // sets rid

		String addr = this.server + ":" + this.port;
		sendPacket("<body to=\"" + this.host + "\" hold=\"1\" wait=\"" + wait + "\" rid=\"" + rid + "\" "
				+ "xml:lang=\"en\" " + (this.use_ssl == true ? "secure=\"true\" " : "") + ""
				+ (addr.substring(0, addr.indexOf(":")).equals(this.host) ? "" : "route=\"xmpp:" + addr + "\" ")
				+ "xmlns=\"http://jabber.org/protocol/httpbind\" " + "/>", 0);

		XmlReader reader = null;
		try {
			reader = this.readResponse(0);		
			if (reader != null) {				
				while (reader.next() == XmlReader.START_TAG) {
					if (reader.getName().equals("body")) {
						sid = reader.getAttribute("sid");
						if (sid.length() == 0) {
							this.packetParser.parseIgnore(reader);
							throw new Exception("Session ID not given!");
						}

						if (reader.getAttribute("requests").equals("1")) {
							this.packetParser.parseIgnore(reader);
							throw new Exception("Server supports only polling behaviour!");
						}

						// adjust wait attribute
						int tmpWait = Integer.parseInt(reader.getAttribute("wait"));
						if (tmpWait < wait)
							wait = tmpWait;

					} else if (reader.getName().equals("stream:features")) {
						this.packetParser.parseFeatures(reader);

					} else {
						this.packetParser.parseIgnore(reader);

					}
				}
			}	

		} finally {
			if (reader != null) {
				reader.close();
				reader = null;
			}			
		}
	}

	protected boolean login() throws Exception {
		boolean loginSuccess = false;
		try {
			/** Authenticate */
			loginSuccess = this.doAuthentication();


			/** Bind Resource */
			sendPacketAndParseResponse("<iq type=\"set\" id=\"res_binding_bosh\">"
					+ "<bind xmlns=\"urn:ietf:params:xml:ns:xmpp-bind\">"
					+ "<resource>" + resource + "</resource></bind></iq>");
			readAndHandleMultipleStanza(0);


			/** Open Session */
			sendPacketAndParseResponse("<iq to=\"" + this.host + "\" type=\"set\" id=\"session_binding\">" + "<session xmlns=\"urn:ietf:params:xml:ns:xmpp-session\"/></iq>");
			readAndHandleMultipleStanza(0);


			// go online
			//connection.getRosterObject().getProfile().setOffline(0);

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}

		return loginSuccess;
	}



	/**
	 * This thread keeps looping until we terminate the session. it writes an
	 * empty request (<body/>) and blocks until the server sends a stanza. the server
	 * should respond with an empty response (<body/>) after <wait> seconds if there
	 * is nothing to send , or earlier if there is an incoming stanza.
	 * We restart the loop once we have handled the stanza
	 */
	protected void parse() throws Exception {
		while (!ended) {
			if (!terminated && !busy) {
				sendPacketWithBody("", 0);
				readAndHandleMultipleStanza(0);
			} 
		}
	}



	protected boolean doAuthentication() throws Exception {
		boolean loginSuccess = false;
		Vector mechanismList = this.packetParser.getMechanism();
		XmlReader reader = null;

		// TODO: need to check DIGEST-MD5 mechanism whether its working or not
		if (mechanismList.contains("DIGEST-MD5")) {
			String nonce = "";
			String cnonce = "";


			// DIGEST-MD5 authorization doing
			sendPacketAndParseResponse("<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"DIGEST-MD5\"/>");

			try {
				reader = this.readResponse(defaultConn);
				if (reader != null) {
					int eventType = reader.next();
					while(eventType == XmlReader.START_TAG) {
						if (reader.getName().equals("challenge")) {
							reader.next();
							String dec = new String(MD5.toBase64(reader.getText().getBytes()));
							int ind = dec.indexOf("nonce=\"") + 7;
							nonce = dec.substring(ind, dec.indexOf("\"", ind + 1));
							cnonce = "00deadbeef00";
							loginSuccess = true;

						} else if (reader.getName().equals("failure")) {
							loginSuccess = false;
							for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
								XmppListener xl = (XmppListener) e.nextElement();
								xl.onAuthFailed(reader.getName() + ", failed authentication");
							}
							throw new Exception("MD5 auth. error");

						} else {
							this.packetParser.parseIgnore(reader);

						}
					}
				}

			} finally {
				if (reader != null) {
					reader.close();
					reader = null;
				}				
			}


			if (loginSuccess) {
				if (nonce.length() > 0 && cnonce.length() > 0) {
					sendPacketAndParseResponse("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">"
							+ this.packetParser.generateAuthResponse(username, password, host, "xmpp/"
									+ host, nonce, cnonce) + "</response>");

					try {
						reader = this.readResponse(defaultConn);
						if (reader != null) {
							int eventType = reader.next();
							reader = this.readResponse(defaultConn);
							while(eventType == XmlReader.START_TAG) {
								if (reader.getName().equals("failure")) {
									loginSuccess = false;
									for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
										XmppListener xl = (XmppListener) e.nextElement();
										xl.onAuthFailed(reader.getName() + ", failed authentication");
									}
									throw new Exception("MD5 auth. error");
								}
							}
						}

					} finally {
						if (reader != null) {
							reader.close();
							reader = null;
						}
					}
				} else {
					loginSuccess = false;
				}
			}


			if (loginSuccess) {
				sendPacketAndParseResponse("<response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>");
				try {
					reader = this.readResponse(defaultConn);
					if (reader != null) {
						int eventType = reader.next();
						reader = this.readResponse(defaultConn);
						while(eventType == XmlReader.START_TAG) {
							if (reader.getName().equals("failure")) {
								loginSuccess = false;
								for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
									XmppListener xl = (XmppListener) e.nextElement();
									xl.onAuthFailed(reader.getName() + ", failed authentication");
								}
								throw new Exception("MD5 auth. error");
							}
						}
					}

				} finally {
					if (reader != null) {
						reader.close();
						reader = null;
					}
				}
			}
		}


		if (mechanismList.contains("X-GOOGLE-TOKEN") && loginSuccess == false) {
			google = true;
			String resp = this.packetParser.getGoogleToken(this.myjid, this.password);

			sendPacketAndParseResponse("<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"X-GOOGLE-TOKEN\">" + resp + "</auth>");

			try {
				reader = this.readResponse(defaultConn);
				if (reader != null) {
					do {
						reader.next();
					} while ((reader.getType() != XmlReader.END_TAG) && (!reader.getName().equals("success")));

					if (reader.getName().equals("success")) {
						loginSuccess = true;
						while (true) {			
							if ((reader.getType() == XmlReader.END_TAG) && reader.getName().equals("success")) break;
							reader.next();
						}
					} else {
						for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
							XmppListener xl = (XmppListener) e.nextElement();
							xl.onAuthFailed(reader.getName() + ", failed authentication");
						}
						throw new Exception("MD5 auth. error");
					}

				}

			} finally {
				if (reader != null) {
					reader.close();
					reader = null;
				}				
			}
		}		

		if (mechanismList.contains("PLAIN") && loginSuccess == false) {
			String resp = "\0" + this.username + "\0" + this.password;
			sendPacketAndParseResponse("<auth xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\" mechanism=\"PLAIN\">"
					+ MD5.toBase64(resp.getBytes()) + "</auth>");

			try {
				reader = this.readResponse(defaultConn);
				if (reader != null) {
					do {
						reader.next();
					} while ((reader.getType() != XmlReader.END_TAG) && (!reader.getName().equals("success")));

					if (reader.getName().equals("success")) {
						loginSuccess = true;
						while (true) {			
							if ((reader.getType() == XmlReader.END_TAG) && reader.getName().equals("success")) break;
							reader.next();
						}
					} else {
						for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
							XmppListener xl = (XmppListener) e.nextElement();
							xl.onAuthFailed(reader.getName() + ", failed authentication");
						}
						throw new Exception("PLAIN authorization error");
					}
				}
			} finally {
				if (reader != null) {
					reader.close();
					reader = null;
				}				
			}
		} 

		else {
			for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
				XmppListener xl = (XmppListener) e.nextElement();
				xl.onAuthFailed("Unknown authorization mechanism");
			}
			throw new Exception("Unknown authorization mechanism");
		}

		return loginSuccess;
	}













	public synchronized void getRosterVCard(String tojid) throws IOException {
		sendPacketAndParseResponse("<iq type=\"get\" id=\"vc2\" to=\"" + tojid + "\">" + "<vCard xmlns=\"vcard-temp\"/></iq>");
		/*if (defaultConn == 0)
			readAndHandleMultipleStanza(defaultConn);*/
	} 


	/**
	 * Sends a roster query.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	public void getRoster() throws IOException {
		//Requests roster items (for all servers)
		sendPacketAndParseResponse("<iq type=\"get\" id=\"roster\">" + "<query xmlns=\"jabber:iq:roster\"/></iq>");
		readAndHandleMultipleStanza(0);
	}

	/**
	 * Sends a message text to a known jid.
	 * 
	 * @param to the JID of the recipient
	 * @param msg the message itself
	 */
	public void sendMessage(String to, String message, final String id) {
		String msgString = "<message to=\""+to+"\" from=\""+this.myjid+"\" type=\"chat\" id=\""+id+"\" xmlns=\"jabber:client\">"+(this.isGoogle()?"<nos:x value=\"disabled\" xmlns:nos=\"google:nosave\"/>":"")+"<body>"+message.trim()+"</body></message>";
		this.sendPacketWithoutParsingResponse(msgString);
	}

	/**
	 * Requesting a subscription.
	 * 
	 * @param to the jid you want to subscribe
	 */
	public void subscribe(String to) {
		this.sendPresence(to, "subscribe", null, null, 0);
	}

	/**
	 * Remove a subscription.
	 * 
	 * @param to the jid you want to remove your subscription
	 */
	public void unsubscribe(String to) {
		this.sendPresence(to, "unsubscribe", null, null, 0);
	}

	/**
	 * Approve a subscription request.
	 * 
	 * @param to the jid that sent you a subscription request
	 */
	public void subscribed(String to) {
		this.sendPresence(to, "subscribed", null, null, 0);
	}	

	/**
	 * Refuse/Reject a subscription request.
	 * 
	 * @param to the jid that sent you a subscription request
	 */
	public void unsubscribed(String to) {
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
	public void setStatus(String show, String status, int priority) {
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
	public void sendPresence(String to, String type, String show, String status, int priority) {
		String msg = "<presence";
		if (type != null) {
			msg += " type=\"" + type + "\"";
		}
		if (to != null) {
			msg += " to=\"" + to + "\"";
		}
		msg += ">";
		if (show != null) {
			msg += "<show>";
			msg += show;
			msg += "</show>";
		}
		if (status != null) {
			msg += "<status>";
			msg += status;
			msg += "</status>";
		}
		if (priority != 0) {
			msg += "<priority>";
			msg += Integer.toString(priority);
			msg += "</priority>";
		}
		msg += "</presence>";

		sendPacketAndParseResponse(msg);
		readAndHandleMultipleStanza(0);
	}

	/**
	 * Closes the stream-tag and the {@link XmlWriter}.
	 */
	public void logoff() {
		//close open conections
		for (int i = 0; i < conn.length; i++) {
			try {
				if (conn[i] != null)
					conn[i].close();
			} catch (Exception e) {
			}	
		}
	}



















	/**
	 * The main parse methode is parsing all types of XML stanzas
	 * <code>message</code>, <code>presence</code> and <code>iq</code>.
	 * Although ignores any other type of xml.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	private synchronized void readAndHandleMultipleStanza(int connIdx) {
		XmlReader reader = readResponse(connIdx);
		if (reader != null) {
			try {
				if (!use_ssl)
					reader.next(); // start tag
				while (reader.next() == XmlReader.START_TAG) {
					final String tmp = reader.getName();
					if (tmp.equals("body")) {
						// ignore the container root element
					} else if (tmp.equals("message")) {
						this.packetParser.parseMessage(reader);
					} else if (tmp.equals("presence")) {
						this.packetParser.parsePresence(reader);
					} else if (tmp.equals("iq")) {
						this.packetParser.parseIq(reader, null);
					} else {
						this.packetParser.parseIgnore(reader);
					}
				}
			} catch (IOException e) {
				ended = true;

			} finally {
				if (reader != null) {
					reader.close();
					reader = null;
				}				
			}
		}
	}

	/**
	 * reads the returned response incl. the enclosing body element
	 * 
	 * @return
	 */
	private XmlReader readResponse(int connIdx) {
		busy = true;
		XmlReader reader = null;
		if (ended) {
			terminate();
			return reader;
		}
		if (!ended) {
			InputStream is = null;
			try {
				HttpConnection httpconn = conn[connIdx];
				int rc = ((HttpConnection)httpconn).getResponseCode();
				if (rc != 200)
					throw new Exception("Unexpected response code: " + rc);
				is = httpconn.openInputStream();





				reader = new XmlReader(is);

			} catch (Exception e) {
				ended = true;
			}/* finally {
				try {
					if (is != null)
						is.close();
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}*/
			busy = false;
		}

		return reader;
	}




	protected void sendPacketAndParseResponse(final String mess) {
		//default connection to use when called from outside
		if (defaultConn == 1) {
			//allow max 2 threads / httpconns
			while (secondThread != null && secondThread.isAlive()) {
				try {
					sleep(200);
				} catch (InterruptedException e) {
				}
				//System.out.println("### WAITED FOR THREAD TO FINISH");
			}
			secondThread = new Thread() {
				public void run() {
					sendPacketWithBody(mess, defaultConn);
					readAndHandleMultipleStanza(defaultConn);
				}
			};
			secondThread.start();
		} else {
			sendPacketWithBody(mess, defaultConn);
		}
	}


	/**
	 * writes the message to the outputsream of the given connection
	 * @param mess
	 * @param conn
	 */
	private void sendPacketWithBody(String mess, int connIdx)  {
		sendPacket("<body rid=\"" + (++rid) + "\" sid=\"" + sid + "\" "
				+ "xmlns=\"http://jabber.org/protocol/httpbind\">" + mess
				+ "</body>", connIdx);
	}


	/**
	 * sends the message, but does not automatically include the enclosing body
	 * element.
	 * 
	 * @param mess
	 */
	private void sendPacket(String mess, int connIdx) {
		if (ended) {
			terminate();
			return;
		}
		OutputStream out = null;
		try {
			byte[] bout = XMPPUtils.ToUTF(mess).getBytes();
			//HttpConnection httpconn = (HttpConnection) Connector.open(httpurl);
			ConnectionFactory connectionFactory = new ConnectionFactory(httpurl, this.connectionMaskIndex);
			HttpConnection httpconn = null;
			try {
				httpconn = (HttpConnection) connectionFactory.getNextConnection();

			} catch (NoMoreTransportsException e) {
				throw new Exception("Connection failed. No transport available.");

			} catch (ConnectionNotFoundException e) {
				throw new Exception("ConnectionNotFoundException: " + e.getMessage());

			} catch (IllegalArgumentException e) {
				throw new Exception("IllegalArgumentException: " + e.getMessage());

			} catch (IOException e) {
				throw new Exception("IOException: " + e.getMessage());

			}


			conn[connIdx] = httpconn;
			// for MIDP1.0 device which do not support HTTP 1.1 by default
			// ((HttpConnection)conn).setRequestProperty("Connection",
			// "keep-alive");
			if (!httpurl.startsWith("https://")) {
				// O2 WAP Flat stuff, only needed for unencrypted http
				httpconn.setRequestProperty("User-Agent",
				"Profile/MIDP-2.0 Configuration/CLDC-1.1");
				httpconn.setRequestProperty("X-WAP-Profile",
				"bla");
			}
			httpconn.setRequestMethod("POST");
			httpconn.setRequestProperty("Content-Length", ""
					+ bout.length);
			out = httpconn.openOutputStream();
			if (out != null) {
				out.write(bout);
				// os.flush(); //don't flush, because punjab can't handle
				// transfer-encoding: chunked
			}

		} catch (Exception e) {
			ended = true;

		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException e) {
				// e.printStackTrace();
			}

		}
	}





	protected void sendPacketWithoutParsingResponse(final String mess) {
		//default connection to use when called from outside
		if (defaultConn == 1) {
			//allow max 2 threads / httpconns
			while (secondThread != null && secondThread.isAlive()) {
				try {
					sleep(200);
				} catch (InterruptedException e) {
				}
				//System.out.println("############### WAITED FOR THREAD TO FINISH");
			}

			secondThread = new Thread() {
				public void run() {
					sendPacketWithBody(mess, defaultConn);

					InputStream is = null;
					try {
						HttpConnection httpconn = conn[defaultConn];
						int rc = ((HttpConnection)httpconn).getResponseCode();
						if (rc != 200)
							throw new Exception("Unexpected response code: " + rc);
						is = httpconn.openInputStream();
					} catch (Exception e) {
						e.printStackTrace();
						ended = true;
					} finally {
						try {
							if (is != null)
								is.close();
						} catch (IOException e) {
							 e.printStackTrace();
						}
					}
				}
			};
			secondThread.start();
		} else {
			sendPacketWithBody(mess, defaultConn);
		}
	}







	/**
	 * @return Returns the ended.
	 */
	public boolean isEnded() {
		return ended;
	}


	/**
	 * @return true if we use google specific services.
	 */
	protected boolean isGoogle() {
		return google;
	}






	public void terminate() {
		if (terminating) 
			return;
		terminating = true;

		//terminate gracefully if we are already connected
		if (defaultConn == 1) {
			//sendPacketAndParseResponse("<presence type=\"unavailable\" xmlns=\"jabber:client\"/>");
			this.sendPresence(null, "unavailable", null, null, priority);
			try {
				//wait for request to be sent
				sleep(4000);
			} catch (InterruptedException e3) {
				e3.printStackTrace();
			}
		}

		if (terminated) {
			this.connectionFailed();
			return;
		}
		try {
			this.connectionFailed("Oops! Something went wrong.");
		} catch (Exception e2) {
		}

		wait = DEFAULT_WAIT;
		rid = -1;
		sid = null;
		ended = true;
		terminated = true;
		defaultConn = 0;

		//close open conections
		for (int i = 0; i < conn.length; i++) {
			try {
				if (conn[i] != null)
					conn[i].close();
			} catch (Exception e) {
			}	
		}
	}



	/**
	 * This method is used to be called on a parser or a connection error.
	 * It tries to close the XML-Reader and XML-Writer one last time.
	 *
	 */
	private void connectionFailed() {
		for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
			XmppListener xl = (XmppListener) e.nextElement();
			xl.onConnFailed("");
		}
	}
	private void connectionFailed(final String msg) {
		for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
			XmppListener xl = (XmppListener) e.nextElement();
			xl.onConnFailed(msg);
		}
	}	
}
