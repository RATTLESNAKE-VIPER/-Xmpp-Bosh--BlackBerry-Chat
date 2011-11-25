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

import java.io.*;
import java.util.*;

/**
 * XML-Writer
 * 
 * @author Grzegorz Grasza
 * @version 1.0
 * @since 1.0
 */
public class XmlWriter {
	private OutputStreamWriter writer;

	private Stack tags;

	boolean inside_tag;

	public XmlWriter(final OutputStream out) throws UnsupportedEncodingException {
		writer = new OutputStreamWriter(out, "UTF-8");
		this.tags = new Stack();
		this.inside_tag = false;
	}
	
	public void close() {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {}
		}		
	}

	public void flush() throws IOException {
		if (this.inside_tag) {
			writer.write('>'); // prevent Invalid XML fatal error
			this.inside_tag = false;
		}
		writer.flush();
	}

	public void startTag(final String tag) throws IOException {
		if (this.inside_tag) {
			writer.write('>');
		}

		writer.write('<');
		writer.write(tag);
		this.tags.push(tag);
		this.inside_tag = true;
	}

	public void attribute(final String atr, final String value) throws IOException {
		if (value == null) { return; }
		writer.write(' ');
		writer.write(atr);
		writer.write("=\'");
		this.writeEscaped(value);
		writer.write('\'');
	}

	public void endTag() throws IOException {
		try {
			final String tagname = (String) this.tags.pop();
			if (this.inside_tag) {
				writer.write("/>");
				this.inside_tag = false;
			} else {
				writer.write("</");
				writer.write(tagname);
				writer.write('>');
			}
		} catch (final EmptyStackException e) {
		}
	}

	public void text(final String str) throws IOException {
		if (this.inside_tag) {
			writer.write('>');
			this.inside_tag = false;
		}
		//this.writeEscaped(this.encodeUTF(str));
		this.writeEscaped(str);
	}

	private void writeEscaped(final String str) throws IOException {
		final int index = 0;
		for (int i = 0; i < str.length(); i++) {
			final char c = str.charAt(i);
			// JXA forget breaks here, issue 23&24
			switch (c) {
				case '<':
					writer.write("&lt;");
					break;
				case '>':
					writer.write("&gt;");
					break;
				case '&':
					writer.write("&amp;");
					break;
				case '\'':
					writer.write("&apos;");
					break;
				case '"':
					writer.write("&quot;");
					break;
				default:
					writer.write(c);
					break;
			}
		}
	}

	private String encodeUTF(final String str) {
		try {
			final String utf = new String(str.getBytes("ISO-8859-1"));
			return utf;
		} catch (final UnsupportedEncodingException e) {
			return null;
		}
	}
};
