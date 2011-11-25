package com.raweng.xmppservice;

import java.io.DataInputStream;
import java.util.Random;
import java.util.Vector;

public class XMPPUtils {
	
	
	/**
	 * generates a random rid with max. 10 digits. note: rid must not exceed
	 * 9007199254740991 during the session
	 * 
	 * @return
	 */
	public static long generateInitialRequestId() {
		String strRid = "";
		Random r = new Random();
		for (int i = 0; i < 10; i++)
			strRid += "" + r.nextInt(10);
		long rid = Long.parseLong(strRid);
		//log("initial rid: " + strRid);
		return rid;
	}
	
	
	
	
	/**
	 * Converts String to UTF-8 String. Code from Colibry IM messenger used
	 * 
	 * @param s
	 *            String to convert
	 * @return converted String
	 */
	public static String ToUTF(String s) {
		int i = 0;
		StringBuffer stringbuffer = new StringBuffer();

		for (int j = s.length(); i < j; i++) {
			int c = (int) s.charAt(i);
			if ((c >= 1) && (c <= 0x7f)) {
				stringbuffer.append((char) c);
			}
			if (((c >= 0x80) && (c <= 0x7ff)) || (c == 0)) {
				stringbuffer.append((char) (0xc0 | (0x1f & (c >> 6))));
				stringbuffer.append((char) (0x80 | (0x3f & c)));
			}
			if ((c >= 0x800) && (c <= 0xffff)) {
				stringbuffer.append(((char) (0xe0 | (0x0f & (c >> 12)))));
				stringbuffer.append((char) (0x80 | (0x3f & (c >> 6))));
				stringbuffer.append(((char) (0x80 | (0x3f & c))));
			}
		}

		return stringbuffer.toString();
	}
	
	
	
	
	/**
	 * Base16 encodes input string
	 * 
	 * @param s
	 *            input string
	 * @return output string
	 */
	public String Base16Encode(String s) {
		String res = "";
		for (int i = 0; i < s.length(); i++) {
			res += Integer.toHexString(s.charAt(i));
		}
		return res;
	}
	
	
	
	
	
	/**
	 * MD5 routines
	 * 
	 * @param s
	 * @return
	 */
	public static byte[] md5It(String s) {
		byte bb[] = new byte[16];
		try {
			MD5 md2 = new MD5(s.getBytes());
			return md2.doFinal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bb;
	}

	/**
	 * MD5 routines
	 * 
	 * @param s
	 * @param l
	 * @return
	 */
	public static byte[] md5It(byte[] s, int l) {
		byte bb[] = new byte[16];
		try {
			byte tmp[] = new byte[l];
			for (int i = 0; i < l; i++) {
				tmp[i] = s[i];
			}
			MD5 md2 = new MD5(tmp);
			return md2.doFinal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bb;
	}
	
	
	
	
	/**
	 * Service routine
	 * 
	 * @param dis
	 * @return
	 */
	public static String readLine(DataInputStream dis) {
		String s = "";
		byte ch = 0;
		try {
			while ((ch = dis.readByte()) != -1) {
				// System.out.println("ch = "+ch);
				if (ch == '\n')
					return s;
				s += (char) ch;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
	}
	
	
	
	
	/**
	 * URL-encodes the given string
	 * @param s
	 * @return
	 */
	public static String URLencode(String s)
	{
		if (s!=null) {
			StringBuffer tmp = new StringBuffer();
			int i=0;
			try {
				while (true) {
					int b = (int)s.charAt(i++);
					if ((b>=0x30 && b<=0x39) || (b>=0x41 && b<=0x5A) || (b>=0x61 && b<=0x7A)) {
						tmp.append((char)b);
					}
					else {
						tmp.append("%");
						if (b <= 0xf) tmp.append("0");
						tmp.append(Integer.toHexString(b));
					}
				}
			}
			catch (Exception e) {}
			return tmp.toString();
		}
		return null;
	}
	
	
	
	
	
	
	
	public static Vector sort(String[] e) {
        Vector v = new Vector();
        for(int count = 0; count < e.length; count++) {
            String s = e[count];
            int i = 0;
            for (i = 0; i < v.size(); i++) {
                int c = s.compareTo((String) v.elementAt(i));
                if (c < 0) {
                    v.insertElementAt(s, i);
                    break;
                } else if (c == 0) {
                    break;
                }
            }
            if (i >= v.size()) {
                v.addElement(s);
            }
        }
        return v;
    }

	

	public static void bubbleSort(String[] p_array) throws Exception
	{
		boolean anyCellSorted;
		int length = p_array.length;
		String tmp;
		for (int i = length; --i >= 0;)
		{
			anyCellSorted = false;
			for (int j = 0; j < i; j++)
			{
				if (p_array[j].compareTo(p_array[j + 1]) > 0)
				{
					tmp = p_array[j];
					p_array[j] = p_array[j + 1];
					p_array[j + 1] = tmp;
					anyCellSorted = true;
				}

			}
			if (anyCellSorted == false)
			{
				return;
			}
		}
	}

}
