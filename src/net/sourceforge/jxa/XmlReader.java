/*
 * Copyright 2004 Grzegorz Grasza groz@gryf.info
 * 
 * This file is part of mobber. Mobber is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version. Mobber is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of
 * the GNU General Public License along with mobber; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA .
 */

package net.sourceforge.jxa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * XML-Reader
 * 
 * @author Grzegorz Grasza
 * @version 1.0
 * @since 1.0
 */
public class XmlReader {	
	private InputStream is;

	public final static int START_DOCUMENT = 0;

	public final static int END_DOCUMENT = 1;

	public final static int START_TAG = 2;

	public final static int END_TAG = 3;

	public final static int TEXT = 4;

	//private Stack tags;

	private boolean inside_tag;
	
	private boolean left_angle;

	private String tagName;

	private String text;

	private final Hashtable attributes = new Hashtable();

	private int c;

	private int type = START_DOCUMENT;

	//public XmlReader(final InputStream in) throws IOException, UnsupportedEncodingException {
	public XmlReader(final InputStream in) throws IOException {
		//reader = new InputStreamReader(in, "UTF-8");
		this.is = in;
		//this.tags = new Stack();
		this.inside_tag = false;
		this.left_angle = false;
	}
	
	//http://discussion.forum.nokia.com/forum/showthread.php?t=76814
	//by abirr
	private int getNextCharacter() throws IOException {
		int a = is.read();
		int t=a;

		if((t|0xC0)==t){
			int b = is.read();
			if( b == 0xFF ){ // Check if legal
				t=-1;
			}else if( b < 0x80 ){ // Check for UTF8 compliancy
				throw new IOException("Bad UTF-8 Encoding encountered");
			}else if((t|0xE0)==t) {
				int c = is.read();
				if( c == 0xFF ){ // Check if legal
					t=-1;
				}else if( c < 0x80 ){ // Check for UTF8 compliancy
					throw new IOException("Bad UTF-8 Encoding encountered");
				}else
					t=((a & 0x0F)<<12) | ((b & 0x3F)<<6) | (c & 0x3F);
			}else
				t=((a & 0x1F)<<6)|(b&0x3F);
		}
		return t;
	}
	
	public void close() {
		if (is != null) {
			try {
				is.close();
				is = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
			/*try {
				reader.close();
			} catch (IOException e) {}*/
		}		
	}

	public int next() throws IOException {
/*		while (!this.ready())
			try { 
				java.lang.Thread.sleep(100);
			} catch (InterruptedException e) {}*/
		this.c = getNextCharacter();
		if (this.c <= ' ') {
			while (((this.c = getNextCharacter()) <= ' ') && (this.c != -1)) {
				;
			}
		}
		if (this.c == -1) {
			this.type = END_DOCUMENT;
			return this.type;
		}

		if (this.left_angle || (this.c == '<')) {
			this.inside_tag = true;
			// reset all
			this.tagName = null;
			this.text = null;
			this.attributes.clear();

			if (this.c == '<') {
				this.left_angle = true;
				this.c = getNextCharacter();
			}
			if (this.left_angle && this.c == '/') {
				this.left_angle = false;
				this.type = END_TAG;
				this.c = getNextCharacter();
				this.tagName = this.readName('>');
			} else if (this.left_angle && ((this.c == '?') || (this.c == '!'))) {// ignore xml heading & // comments
				this.left_angle = false;
				while ((this.c = getNextCharacter()) != '>') {
					;
				}
				this.next();
			} else {
				this.left_angle = false;
				this.type = START_TAG;
				this.tagName = this.readName(' ');

				String attribute = "";
				String value = "";
				while (this.c == ' ') {
					/*this.c = getNextCharacter();
					attribute = this.readName('=');

					int quote = getNextCharacter();//this.c = this.read(); // '''
					BTalk.debugConsole.addDebugMsg("quote: " + quote);
					this.c = getNextCharacter();
					value = this.readText(quote); //change from value = this.readText(''');
					this.c = getNextCharacter();
					this.attributes.put(attribute, value);
					BTalk.debugConsole.addDebugMsg("attributes: " + attributes);*/
					
					
					/**
					 * 
					 * Temporary solution... Need better way to resolve the character issue (@Gautam)
					 * 
					 */
					this.c = getNextCharacter();
					attribute = this.readName('=').trim();					
					
					int quote = getNextCharacter();//this.c = this.read(); // '''
					if (quote == 32) {
						while (quote == 32) {
							quote = getNextCharacter();//this.c = this.read(); // '''
							
						}
						this.c = getNextCharacter();
						value = this.readText(quote); //change from value = this.readText(''');
						this.c = getNextCharacter();
						this.attributes.put(attribute, value);	
						
					} else {
						this.c = getNextCharacter();
						value = this.readText(quote); //change from value = this.readText(''');
						this.c = getNextCharacter();
						this.attributes.put(attribute, value);	
					}
					
				}
				if (this.c != '/') {
					this.inside_tag = false;
				}
			}
		} else if ((this.c == '>') && this.inside_tag) // last tag ended
		{
			this.type = END_TAG;
			this.inside_tag = false;
		} else {
			this.tagName = null;
			this.attributes.clear();

			this.type = TEXT;
			this.text = this.readText('<');
			// fix the < dismatching problem
			this.left_angle = true;
		}

		return this.type;
	}
	
	// NOTICE: this is only for debug use
	public void parseHtml() throws IOException {
		while (true) {
			char c;
			c = (char) this.getNextCharacter();
			System.out.print(c);
		}
	}

	public int getType() {
		return this.type;
	}

	public String getName() {
		return this.tagName;
	}

	public String getAttribute(final String name) {
		return (String) this.attributes.get(name);
	}

	public Enumeration getAttributes() {
		return this.attributes.keys();
	}

	public String getText() {
		return this.text;
	}

	private String readText(final int end) throws IOException {
		final StringBuffer output = new StringBuffer("");
		while (this.c != end) {
			if (this.c == '&') {
				this.c = getNextCharacter();
				switch (this.c) {
					case 'l':
						output.append('<');
						break;
					case 'g':
						output.append('>');
						break;
					case 'a':
						if (getNextCharacter() == 'm') {
							output.append('&');
						} else {
							output.append('\'');
						}
						break;
					case 'q':
						output.append('"');
						break;
					case 'n':
						output.append(' ');
						break;
					default:
						output.append('?');
				}

				while ((this.c = getNextCharacter()) != ';') {
					;
				}
			// NOTICE: Comment out these mystical codes
//			} else if (this.c == '\\') {
//				// NOTICE: What this means?
//				if ((this.c = getNextCharacter()) == '<') {
//					output.append('\\');
//					break;
//				} else {
//					output.append((char) this.c);
//				}
			} else {
				output.append((char) this.c);
			}
			this.c = getNextCharacter();
		}
		// while((c = read()) != end);
		return output.toString();
	}

	private String readName(final int end) throws IOException {
		final StringBuffer output = new StringBuffer("");
		do {
			output.append((char) this.c);
		} while (((this.c = getNextCharacter()) != end) && (this.c != '>') && (this.c != '/'));
		return output.toString();
	}
};
