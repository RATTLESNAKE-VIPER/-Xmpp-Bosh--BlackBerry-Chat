package com.raweng.xmppservice;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.HttpConnection;

import net.sourceforge.jxa.XmlReader;
import net.sourceforge.jxa.XmlWriter;


public class PacketParser {

	private XMPPThread xmppThread;
	Vector mechanismList = new Vector();

	public PacketParser(XMPPThread xmppThread) {
		this.xmppThread = xmppThread;
	}
	
	
	/**
	 * This method parses all info/query stanzas, including authentication
	 * mechanism and roster. It also answers version queries.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	public void parseIq(final XmlReader reader, XmlWriter writer) throws IOException {
		String type = reader.getAttribute("type");
		final String id = reader.getAttribute("id");
		final String from = reader.getAttribute("from");
		
		if (type.equals("error")) {			
			if (!id.equals("vc2")) {
				while (reader.next() == XmlReader.START_TAG) {
					if (reader.getName().equals("error")) {						
						final String code = reader.getAttribute("code");

						for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
							XmppListener xl = (XmppListener) e.nextElement();
							xl.onAuthFailed(code + ": " + this.parseText(reader));
						}
					} else {
						this.parseText(reader);
					}
				}
			} else {
				if (id.equals("vc2")) {					
					this.parseText(reader);
					for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
						XmppListener xl = (XmppListener) e.nextElement();
						xl.onRosterVCardEvent(from, null);
					}
				}
			}
			
		} else if ((type.equals("result") && (id != null)) && (id.equals("res_binding") || (id.equals("res_binding_bosh")) || id.equals("session_binding") || id.equals("vc2"))) {
			if (id.equals("res_binding") || (id.equals("res_binding_bosh"))) {
				// authorized
				while (true) {
					reader.next();
					String tagname = reader.getName();
					if (tagname != null) {
						if ((reader.getType() == XmlReader.START_TAG) && tagname.equals("jid")) {
							reader.next();
							String rsp_jid = reader.getText();
							int i = rsp_jid.indexOf('/');
							this.xmppThread.resource = rsp_jid.substring(i+1);
						} else if (tagname.equals("iq")) 
							break;
					}
				}
				if (id.equals("res_binding")) {
					for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
						XmppListener xl = (XmppListener) e.nextElement();
						xl.onAuth(this.xmppThread.resource);
					}
					this.xmppThread.sendPresence(null, null, null, null, this.xmppThread.priority);
				}				
				
			} else if (id.equals("session_binding")) {
				for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
					XmppListener xl = (XmppListener) e.nextElement();
					xl.onAuth(this.xmppThread.resource);
				}
				this.xmppThread.sendPresence(null, null, null, null, this.xmppThread.priority);
				
				
			} else if (id.equals("vc2")) {
				while (reader.next() == XmlReader.START_TAG) {
					final String tmp = reader.getName();
					if (tmp.equals("vCard")) {
						boolean done = false;
						boolean isBinaryValuePresent = false;
						while (!done) {
							int eventType = reader.next();
							
							if (eventType == XmlReader.START_TAG) {
								
								if (reader.getName().equals("BINVAL")) {									
									isBinaryValuePresent = true;
									reader.next();
									String base64StringValue = reader.getText();
									if (base64StringValue != null) {
										for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
											XmppListener xl = (XmppListener) e.nextElement();
											xl.onRosterVCardEvent(from, base64StringValue);
										}
									}
								}
							} else if (eventType == XmlReader.END_TAG) {
								if (reader.getName().equals("vCard")) {
									done = true;
								}
							}
						}	
						if(!isBinaryValuePresent){
							for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
								XmppListener xl = (XmppListener) e.nextElement();
								xl.onRosterVCardEvent(from, null);
							}
						}
					} else {
						this.parseIgnore(reader);
						for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
							XmppListener xl = (XmppListener) e.nextElement();
							xl.onRosterVCardEvent(from, null);
						}
					}
				}
			}			
		} else {
			//java.lang.System.out.println("contacts list");
			while (reader.next() == XmlReader.START_TAG) {
				if (reader.getName().equals("query")) {
					if (reader.getAttribute("xmlns").equals("jabber:iq:roster")) {
						while (reader.next() == XmlReader.START_TAG) {
							if (reader.getName().equals("item")) {
								type = reader.getAttribute("type");
								String jid = reader.getAttribute("jid");
								String name = reader.getAttribute("name");
								String subscription = reader.getAttribute("subscription");
								//newjid = (jid.indexOf('/') == -1) ? jid : jid.substring(0, jid.indexOf('/'));
								boolean check = true;
								
								while (reader.next() == XmlReader.START_TAG) {
									if (reader.getName().equals("group")) {
										for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
											XmppListener xl = (XmppListener) e.nextElement();
											xl.onContactEvent(jid, name, this.parseText(reader), subscription);
										}
										check = false;
									} else {
										this.parseIgnore(reader);
									}
								}
								//if (check && !subscription.equals("remove"))
								if (check) {
									for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
										XmppListener xl = (XmppListener) e.nextElement();
										xl.onContactEvent(jid, name, "", subscription);
									}
								}
							} else {	// !this.reader.getName().equals("item")
								this.parseIgnore(reader);
							}
						}
						for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
							XmppListener xl = (XmppListener) e.nextElement();
							xl.onContactOverEvent();
						}
					} else if (reader.getAttribute("xmlns").equals("jabber:iq:version")) {
						while (reader.next() == XmlReader.START_TAG) {
							this.parseIgnore(reader);
						}
						// reader.next();
						// send version
						if (writer != null) {
							writer.startTag("iq");
							writer.attribute("type", "result");
							writer.attribute("id", id);
							writer.attribute("to", from);
							writer.startTag("query");
							writer.attribute("xmlns", "jabber:iq:version");

							writer.startTag("name");
							writer.text("jxa");
							writer.endTag();
							writer.startTag("version");
							writer.text("1.0");
							writer.endTag();
							writer.startTag("os");
							writer.text("J2ME");
							writer.endTag();

							writer.endTag(); // query
							writer.endTag(); // iq
						}						
					} else {
						this.parseIgnore(reader);
					}
				} else {
					this.parseIgnore(reader);
				}
			}
		}
	}


	/**
	 * This method parses all presence stanzas, including subscription requests.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	public void parsePresence(XmlReader reader) throws IOException {
		final String from = reader.getAttribute("from"), type = reader.getAttribute("type");
		String status = "", show = "";
		// int priority=-1;

		while (reader.next() == XmlReader.START_TAG) {
			final String tmp = reader.getName();
			if (tmp.equals("status")) {
				status = this.parseText(reader);
			} else if (tmp.equals("show")) {
				show = this.parseText(reader);
				// else if(tmp.equals("priority"))
				// priority = Integer.parseInt(parseText());
			} else {
				this.parseIgnore(reader);
			}
		}

		//if ((type != null) && (type.equals("unavailable") || type.equals("unsubscribed") || type.equals("error"))) {
		if (type == null) {
			for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
		      XmppListener xl = (XmppListener) e.nextElement();
		      xl.onStatusEvent(from, show, status);
		   }
		} else {	// type != null
			if (type.equals("unsubscribed") || type.equals("error")) {
				for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
					XmppListener xl = (XmppListener) e.nextElement();
					xl.onUnsubscribeEvent(from);
				}
			} else if (type.equals("subscribe")) {
				for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
					XmppListener xl = (XmppListener) e.nextElement();
					xl.onSubscribeEvent(from);
				}
			} else if (type.equals("unavailable")) {
				//final String jid = (from.indexOf('/') == -1) ? from : from.substring(0, from.indexOf('/'));
				for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
					XmppListener xl = (XmppListener) e.nextElement();
					//xl.onStatusEvent(jid, show, status);
					xl.onStatusEvent(from, "na", status);
				}
			}	// end type.equals
		} // end type == null
	}
	
	
	
	
	/**
	 * This method parses all incoming messages.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	public void parseMessage(XmlReader reader) throws IOException {
		final String from = reader.getAttribute("from"), to = reader.getAttribute("to"), id = reader.getAttribute("id"), type = reader.getAttribute("type");
		String body = null, subject = null;
		int tagTemp;
		while ((tagTemp = reader.next()) == XmlReader.START_TAG ||
				tagTemp == XmlReader.TEXT) {
			if (tagTemp == XmlReader.TEXT) {
				continue;
			}
			final String tmp = reader.getName();
			// TODO add more tmp dealing
			if (tmp.equals("body")) {
				body = this.parseText(reader);
			} else if (tmp.equals("subject")) {
				subject = this.parseText(reader);
			} else if (tmp.equals("html")) {
				this.parseHtml(reader);
			} else if (tmp.equals("x")) {
				this.parseTimeStamp(reader);
			} else {
				this.parseIgnore(reader);
			}
		}
		// (from, subject, body);
		for (Enumeration e = this.xmppThread.listeners.elements(); e.hasMoreElements();) {
			XmppListener xl = (XmppListener) e.nextElement();
			if (body != null) {
				xl.onMessageEvent((from.indexOf('/') == -1) ? from : from.substring(0, from.indexOf('/')), to, body, id);
			}
		}
		
		while (reader.getType() != XmlReader.END_TAG || !reader.getName().equals("message")) {
			reader.next();
		}
	}
	
	
	
	
	/**
	 * This method parses all text inside of xml start and end tags.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	public String parseText(XmlReader reader) throws IOException {
		final String endTagName = reader.getName();
		final StringBuffer str = new StringBuffer("");
		int t = reader.next(); // omit start tag
		while (!endTagName.equals(reader.getName())) {
			if (t == XmlReader.TEXT) {
				str.append(reader.getText());
			}
			t = reader.next();
		}
		return str.toString();
	}
	
	
	
	/**
	 * This method doesn't parse tags it only let the reader go through unknown
	 * tags.
	 * 
	 * @throws java.io.IOException is thrown if {@link XmlReader} or {@link XmlWriter}
	 *	throw an IOException.
	 */
	public void parseIgnore(XmlReader reader) throws IOException {
		int x;
		while ((x = reader.next()) != XmlReader.END_TAG) {
			if (x == XmlReader.START_TAG) {
				this.parseIgnore(reader);
			}
		}
	}
	
	
	public void parseTimeStamp(XmlReader reader) {
		// TODO Auto-generated method stub
		
	}

	public void parseHtml(XmlReader reader) throws IOException {
		// FIXME just ignore the html area
//		int t;
//		String s1, s2;
//		t = this.reader.next();
//		s1 = this.reader.getName();
//		s2 = this.reader.getText();
		reader.next();
		while (!"html".equals(reader.getName())) {
			reader.next();
//			t = this.reader.next();
//			s1 = this.reader.getName();
//			s2 = this.reader.getText();
		}
		return;
//		this.reader.parseHtml();
	}
	
	
	
	
	
	
	
	
	
	/**
	 * Parse and setup the XML stream features.
	 * 
	 * @param reader the XML parser, positioned at the start of a message packet.
	 * @throws Exception if an exception occurs while parsing the packet.
	 */
	public void parseFeatures(XmlReader reader) throws Exception {
		boolean done = false;
		while (!done) {
			int eventType = reader.next();

			if (eventType == XmlReader.START_TAG) {
				if (reader.getName().equals("mechanisms")) {
					// The server is reporting available SASL mechanisms. 
					//Store this information which will be used later while logging (i.e. authenticating) into the server
					parseMechanisms(reader);
					
				} else if (reader.getName().equals("bind")) {
					// The server requires the client to bind a resource to the stream
					
				} else if (reader.getName().equals("register")) {
					
					
				}
			} else if (eventType == XmlReader.END_TAG) {
				if (reader.getName().equals("stream:features")) {
					done = true;
				}
			}
		}
	}


	public void parseMechanisms(XmlReader reader) throws Exception {
		int eventType = reader.next();
		if (eventType == XmlReader.START_TAG) {
			if (reader.getName().equals("mechanism")) {
				if (reader.next() == XmlReader.TEXT) {
					String value = reader.getText();
					if (value != null) {
						/*if (value.equals("DIGEST-MD5")) {   
							mechanismList.addElement("DIGEST-MD5");

						} if (value.equals("X-GOOGLE-TOKEN")) {   
							mechanismList.addElement("X-GOOGLE-TOKEN");

						} if (value.equals("PLAIN")) {   
							mechanismList.addElement("PLAIN");
						}*/
						
						mechanismList.addElement(value);
					}  
				}
			}
		}
	}
	
	
	
	
	public Vector getMechanism() throws Exception {
		return mechanismList;		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * Generates X-GOOGLE-TOKEN response by communication with
	 * http://www.google.com
	 * 
	 * @param userName
	 * @param passwd
	 * @return
	 */
	protected String getGoogleToken(String jid, String passwd) {
		String first = "Email=" + XMPPUtils.URLencode(jid) + "&Passwd=" + XMPPUtils.URLencode(passwd)
				+ "&PersistentCookie=false&source=googletalk";
		
		HttpConnection c = null;
		DataInputStream dis = null;
		OutputStream os = null;
		try {
			ConnectionFactory connectionFactory = new ConnectionFactory("https://www.google.com:443/accounts/ClientAuth");

			try {
				c = (HttpConnection) connectionFactory.getNextConnection();
				c.setRequestMethod("POST");
				c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				c.setRequestProperty("Content-Length", ""+first.getBytes());

			} catch (NoMoreTransportsException e) {
				throw new Exception("Connection failed. No transport available.");

			} catch (ConnectionNotFoundException e) {
				throw new Exception("ConnectionNotFoundException: " + e.getMessage());

			} catch (IllegalArgumentException e) {
				throw new Exception("IllegalArgumentException: " + e.getMessage());

			} catch (IOException e) {
				throw new Exception("IOException: " + e.getMessage());

			}
			
			os = c.openOutputStream();
			os.write(first.getBytes());
			os.close();
			
			dis = c.openDataInputStream();			
			String str = XMPPUtils.readLine(dis);
			
			String SID = "";
			String LSID = "";
			if (str.startsWith("SID=")) {
				SID = str.substring(4, str.length());
				str = XMPPUtils.readLine(dis);
				LSID = str.substring(5, str.length());
				
				first = "SID=" + SID + "&LSID=" + LSID
						+ "&service=mail&Session=true";
								
				dis.close();
				c.close();
				
				
				ConnectionFactory connectionFactory2 = new ConnectionFactory("https://www.google.com:443/accounts/IssueAuthToken");
				try {
					c = (HttpConnection) connectionFactory2.getNextConnection();
					c.setRequestMethod("POST");
					c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					c.setRequestProperty("Content-Length", ""+first.getBytes());

				} catch (NoMoreTransportsException e) {
					throw new Exception("Connection failed. No transport available.");

				} catch (ConnectionNotFoundException e) {
					throw new Exception("ConnectionNotFoundException: " + e.getMessage());

				} catch (IllegalArgumentException e) {
					throw new Exception("IllegalArgumentException: " + e.getMessage());

				} catch (IOException e) {
					throw new Exception("IOException: " + e.getMessage());

				}
				
				os = c.openOutputStream();
				os.write(first.getBytes());
				os.close();
				
				dis = c.openDataInputStream();
				str = XMPPUtils.readLine(dis);
				String token = MD5.toBase64(new String("\0" + jid + "\0" + str).getBytes());
				dis.close();
				c.close();
				return token;
			} else {
				throw new Exception("Invalid response: "+str);
			}
				
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try { if(dis!=null) dis.close(); } catch (Exception e) {}
			try { if(os!=null) os.close(); } catch (Exception e) {}
			try { if(c!=null) c.close(); } catch (Exception e) {}
		}
		return "";
	}
	
	
	/**
	 * This routine generates MD5-DIGEST response via SASL specification
	 * 
	 * @param user
	 * @param pass
	 * @param realm
	 * @param digest_uri
	 * @param nonce
	 * @param cnonce
	 * @return
	 */
	protected String generateAuthResponse(String user, String pass,
			String realm, String digest_uri, String nonce, String cnonce) {
		String val1 = user + ":" + realm + ":" + pass;
		byte bb[] = new byte[17];
		bb = XMPPUtils.md5It(val1);
		int sl = new String(":" + nonce + ":" + cnonce).length();
		byte cc[] = new String(":" + nonce + ":" + cnonce).getBytes();
		byte bc[] = new byte[99];
		for (int i = 0; i < 16; i++) {
			bc[i] = bb[i];
		}
		for (int i = 16; i < sl + 16; i++) {
			bc[i] = cc[i - 16];
		}
		String val2 = new String(MD5.toHex(XMPPUtils.md5It(bc, sl + 16)));
		String val3 = "AUTHENTICATE:" + digest_uri;
		val3 = MD5.toHex(XMPPUtils.md5It(val3));
		String val4 = val2 + ":" + nonce + ":00000001:" + cnonce + ":auth:"
		+ val3;
		// System.out.println("Before auth = "+val4+", val1 = "+val1);
		val4 = MD5.toHex(XMPPUtils.md5It(val4));
		// System.out.println("Val4 = "+val4);

		String enc = "charset=utf-8,username=\"" + user + "\",realm=\"" + realm
		+ "\"," + "nonce=\"" + nonce + "\",cnonce=\"" + cnonce + "\","
		+ "nc=00000001,qop=auth,digest-uri=\"" + digest_uri + "\","
		+ "response=" + val4;
		String resp = MD5.toBase64(enc.getBytes());
		return resp;
	}
	
}
