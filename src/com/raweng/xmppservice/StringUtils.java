package com.raweng.xmppservice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Random;

public class StringUtils {

	private static Random randGen = new Random();
	private static char[] numbersAndLetters = "0123456789abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	private static MD5 digest;

	private static final char[] QUOTE_ENCODE = "&quot;".toCharArray();
	private static final char[] APOS_ENCODE = "&apos;".toCharArray();
	private static final char[] AMP_ENCODE = "&amp;".toCharArray();
	private static final char[] LT_ENCODE = "&lt;".toCharArray();
	private static final char[] GT_ENCODE = "&gt;".toCharArray();

	public static synchronized String hash(String paramString)
	{
		if (digest == null) {
			/*try {
				digest = MD5.getInstance("SHA-1");
			}
			catch (NoSuchAlgorithmException localNoSuchAlgorithmException) {
				System.err.println("Failed to load the SHA-1 MessageDigest. Jive will be unable to function normally.");
			}*/
			try {
				digest = new MD5(paramString.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				System.err.println("Failed to load the SHA-1 MessageDigest. Jive will be unable to function normally.");
			}
		}

		try
		{
			digest.update(paramString.getBytes("UTF-8"));
		}
		catch (UnsupportedEncodingException localUnsupportedEncodingException) {
			System.err.println(localUnsupportedEncodingException);
		}
		//return encodeHex(digest.digest());
		return MD5.toHex(digest.doFinal());
	}

	public static String randomString(int paramInt)
	{
		if (paramInt < 1) {
			return null;
		}

		char[] arrayOfChar = new char[paramInt];
		for (int i = 0; i < arrayOfChar.length; i++) {
			arrayOfChar[i] = numbersAndLetters[randGen.nextInt(71)];
		}
		return new String(arrayOfChar);
	}



	public static String escapeNode(String paramString)
	{
		if (paramString == null) {
			return null;
		}
		StringBuffer localStringBuilder = new StringBuffer(paramString.length() + 8);
		int i = 0; for (int j = paramString.length(); i < j; i++) {
			char c = paramString.charAt(i);
			switch (c) { case '"':
				localStringBuilder.append("\\22"); break;
			case '&':
				localStringBuilder.append("\\26"); break;
			case '\'':
				localStringBuilder.append("\\27"); break;
			case '/':
				localStringBuilder.append("\\2f"); break;
			case ':':
				localStringBuilder.append("\\3a"); break;
			case '<':
				localStringBuilder.append("\\3c"); break;
			case '>':
				localStringBuilder.append("\\3e"); break;
			case '@':
				localStringBuilder.append("\\40"); break;
			case '\\':
				localStringBuilder.append("\\5c"); break;
			default:
				/*if (Character.isWhitespace(c)) {
	          localStringBuilder.append("\\20");
	        }
	        else {
	          localStringBuilder.append(c);
	        }*/
				localStringBuilder.append(c);
			}
		}

		return localStringBuilder.toString();
	}

	public static String unescapeNode(String paramString)
	{
		if (paramString == null) {
			return null;
		}
		char[] arrayOfChar = paramString.toCharArray();
		StringBuffer localStringBuilder = new StringBuffer(arrayOfChar.length);
		int i = 0; for (int j = arrayOfChar.length; i < j; i++)
		{
			char c = paramString.charAt(i);
			if ((c == '\\') && (i + 2 < j)) {
				int k = arrayOfChar[(i + 1)];
				int m = arrayOfChar[(i + 2)];
				if (k == 50) {
					switch (m) { case 48:
						localStringBuilder.append(' '); i += 2; break;
					case 50:
						localStringBuilder.append('"'); i += 2; break;
					case 54:
						localStringBuilder.append('&'); i += 2; break;
					case 55:
						localStringBuilder.append('\''); i += 2; break;
					case 102:
						localStringBuilder.append('/'); i += 2; break;
					default:
						break;
					}
				}
				if (k == 51) {
					switch (m) { case 97:
						localStringBuilder.append(':'); i += 2; break;
					case 99:
						localStringBuilder.append('<'); i += 2; break;
					case 101:
						localStringBuilder.append('>'); i += 2; break;
					case 98:
					case 100:
					default:
						break;
					}
				}
				if (k == 52) {
					if (m == 48) {
						localStringBuilder.append("@");
						i += 2;
						continue;
					}
				}
				else if ((k == 53) && 
						(m == 99)) {
					localStringBuilder.append("\\");
					i += 2;
					continue;
				}
			}

			localStringBuilder.append(c);
		}

		return localStringBuilder.toString();
	}

	public static String escapeForXML(String paramString)
	{
		if (paramString == null) {
			return null;
		}

		int j = 0;
		int k = 0;
		char[] arrayOfChar = paramString.toCharArray();
		int m = arrayOfChar.length;
		StringBuffer localStringBuilder = new StringBuffer((int)(m * 1.3D));
		for (; j < m; j++) {
			int i = arrayOfChar[j];
			if (i > 62)
				continue;
			if (i == 60) {
				if (j > k) {
					localStringBuilder.append(arrayOfChar, k, j - k);
				}
				k = j + 1;
				localStringBuilder.append(LT_ENCODE);
			}
			else if (i == 62) {
				if (j > k) {
					localStringBuilder.append(arrayOfChar, k, j - k);
				}
				k = j + 1;
				localStringBuilder.append(GT_ENCODE);
			}
			else if (i == 38) {
				if (j > k) {
					localStringBuilder.append(arrayOfChar, k, j - k);
				}

				if ((m > j + 5) && (arrayOfChar[(j + 1)] == '#') && (Character.isDigit(arrayOfChar[(j + 2)])) && (Character.isDigit(arrayOfChar[(j + 3)])) && (Character.isDigit(arrayOfChar[(j + 4)])) && (arrayOfChar[(j + 5)] == ';'))
				{
					continue;
				}

				k = j + 1;
				localStringBuilder.append(AMP_ENCODE);
			}
			else if (i == 34) {
				if (j > k) {
					localStringBuilder.append(arrayOfChar, k, j - k);
				}
				k = j + 1;
				localStringBuilder.append(QUOTE_ENCODE);
			}
			else if (i == 39) {
				if (j > k) {
					localStringBuilder.append(arrayOfChar, k, j - k);
				}
				k = j + 1;
				localStringBuilder.append(APOS_ENCODE);
			}
		}
		if (k == 0) {
			return paramString;
		}
		if (j > k) {
			localStringBuilder.append(arrayOfChar, k, j - k);
		}
		return localStringBuilder.toString();
	}

	public static String convertStreamToString(InputStream is) {
		byte[] buffer = new byte[1024];
		StringBuffer sb = new StringBuffer();
		int readIn = 0;
		try {
			while((readIn = is.read(buffer)) > 0)
			{
			     String temp = new String(buffer, 0, readIn);
			     sb.append(temp);  
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		String result = sb.toString();
		return result;
	}
	
	public static InputStream fromString(String str)
	{
		byte[] bytes = str.getBytes();
		return new ByteArrayInputStream(bytes);
	}
}
