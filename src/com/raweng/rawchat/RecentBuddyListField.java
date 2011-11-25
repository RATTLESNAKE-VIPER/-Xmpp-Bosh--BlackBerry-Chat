package com.raweng.rawchat;

import java.util.Hashtable;
import java.util.Vector;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.KeypadListener;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.component.ListField;
import net.rim.device.api.ui.component.ObjectListField;


public class RecentBuddyListField extends ObjectListField {
	public static ChatManager chatHandler;
	
	public static final Bitmap unreadIcon = Bitmap.getBitmapResource("unread.png");
	
	private Hashtable recentChatHashMap;
	public Vector recentBuddyVector;
	
	
	public RecentBuddyListField() {
		this.recentChatHashMap = new Hashtable();
		this.recentBuddyVector = new Vector();
		this.setRowHeight(30);
	}
	
	protected boolean navigationClick(int status, int time) {
		int idx = this.getSelectedIndex();
		if (idx >= 0) {
			RecentBuddyListField.chatHandler.currentBuddy = (Buddy)recentBuddyVector.elementAt(idx);
			RecentBuddyListField.chatHandler.openBuddy(RecentBuddyListField.chatHandler.currentBuddy);
		}
		return true;
	}
	
		
	protected boolean keyChar(char key, int status, int time) {
		int idx;
		switch(key) {
		//#KEYMAP
//		case 'd':	//qw
//		case 'o':	//qw
		case 'g':	//st
		case 'h':	//st
		case Keypad.KEY_ENTER:
			idx = this.getSelectedIndex();
			if (idx >= 0) {
				RecentBuddyListField.chatHandler.currentBuddy = (Buddy)recentBuddyVector.elementAt(idx);
				RecentBuddyListField.chatHandler.openBuddy(RecentBuddyListField.chatHandler.currentBuddy);
			}
			return true;
			
		//#KEYMAP
//		case 'k':	//qw
//		case 'e':	//qw
		case 't':	//st
		case 'y':	//st
			idx = this.getSelectedIndex()-1;
			if (idx >= 0) {
				this.setSelectedIndex(idx);
			}
			return true;
			
		case 's':
			return true;
			
		case 'f':
			return true;
		
		//#KEYMAP
//		case 'j':	//qw
//		case 'x':	//qw
		case 'b':	//st
		case 'n':	//st
			idx = this.getSelectedIndex()+1;
			if (idx > 0 && idx < this.getSize()) {
				this.setSelectedIndex(idx);
			}
			return true;
		
		//#KEYMAP
//		case 't':	//qw
		case 'e':	//st
		case 'r':	//st
			if (this.getSize() > 0) {
				this.setSelectedIndex(0);
			}
			return true;
		
		//#KEYMAP
//		case 'b':	//qw
		case 'c':	//st
		case 'v':	//st
			if (this.getSize() > 0) {
				this.setSelectedIndex(this.getSize()-1);
			}
			return true;
		
		// page down
		//#KEYMAP
//		case 'n':	//qw
		case 'm':	//st
		case Keypad.KEY_SPACE:
			ChatManager.buddyscreen.pageDown(1, KeypadListener.STATUS_ALT, time);
			return true;
			
		// page up
		//#KEYMAP
//		case 'p':	//qw
		case 'u':	//st
			ChatManager.buddyscreen.pageUp(-1, KeypadListener.STATUS_ALT, time);
			return true;
			
		}
		
		return false;
	}
	
	
	
	
	public Buddy getBuddyAt(int index) {
		return (Buddy)recentBuddyVector.elementAt(index);		
	}
	
	public int getBuddyIndex(Buddy b) {
		return recentBuddyVector.indexOf(b);
	}	
	
	public void buddyReposition(Buddy b) {
		int index = recentBuddyVector.indexOf(b);
		buddyReposition(index);
	}
	
	public void buddyReposition(int oldIndex) {
		Buddy b = (Buddy)recentBuddyVector.elementAt(oldIndex);
		int newIndex = 0;

		while (newIndex < recentBuddyVector.size() &&
				((b == (Buddy)recentBuddyVector.elementAt(newIndex)) || 
						(b.status < ((Buddy)recentBuddyVector.elementAt(newIndex)).status)))
			++newIndex;

		newIndex = (oldIndex < newIndex) ? (newIndex-1) : newIndex;

		if (oldIndex != newIndex) {
			recentBuddyVector.removeElementAt(oldIndex);
			recentBuddyVector.insertElementAt(b, newIndex);
		}
		this.invalidate();
	}
	
	public int findBuddyIndex(String jid) {
		for (int i = recentBuddyVector.size()-1; i >= 0; i--) {
			if (((Buddy)recentBuddyVector.elementAt(i)).jid.equalsIgnoreCase(jid))
				return i;
		}
		
		return -1;
	}
	
	public Buddy findBuddy(String jid) {
		for (int i = recentBuddyVector.size()-1; i >= 0; i--) {
			if (((Buddy)recentBuddyVector.elementAt(i)).jid.equalsIgnoreCase(jid))
				return (Buddy)recentBuddyVector.elementAt(i);
		}
		return null;
	}
	
	public void addBuddy(Buddy b) {
		recentBuddyVector.addElement(b);
		this.insert(recentBuddyVector.indexOf(b));
	}
	
	public boolean deleteBuddy(String jid) {
		int idx;
		for (idx = recentBuddyVector.size()-1; idx >= 0; idx--) {
			if (((Buddy)recentBuddyVector.elementAt(idx)).jid.equalsIgnoreCase(jid))
				break;
		}
		
		if (idx >= 0) {
			recentBuddyVector.removeElementAt(idx);
			this.delete(idx);
			this.getScreen().invalidate();
			return true;
		} else {
			return false;
		}
	}
	
	public void clearBuddies() {
		if (RecentBuddyListField.chatHandler.buddyList != null) {
			int i = recentBuddyVector.size();
			while (i-- > 0)
				this.delete(0);
			ChatManager.buddyscreen.delete(RecentBuddyListField.chatHandler.buddyList);
			RecentBuddyListField.chatHandler.buddyList = null;
		}
	}
	
	public void invalBuddies() {
		if (RecentBuddyListField.chatHandler.buddyList != null) {
			for (int i = recentBuddyVector.size()-1; i >= 0; i--) {
				Buddy b = (Buddy) recentBuddyVector.elementAt(i);
				b.status = Buddy.STATUS_OFFLINE;
			}
			
			this.invalidate();
		}
	}
	
	
	
	public void drawListRow(ListField listField, Graphics graphics, int index, int y, int width) {
		// Row styling
		{
			if (graphics.isDrawingStyleSet(Graphics.DRAWSTYLE_FOCUS)) {
				graphics.setColor(0x008EC1DA);
				graphics.fillRect(0, y, width, listField.getRowHeight());
			} else {
				graphics.setColor(0x00DBEDF5);
				graphics.fillRect(0, y, width, listField.getRowHeight());
			}
			graphics.setColor(Color.GRAY);	
			graphics.setFont(Font.getDefault().derive(Font.PLAIN, Font.getDefault().getHeight()-6));
		}		
		
		
		// NOTICE 14 would be consist the icon size
		Buddy b = (Buddy)recentBuddyVector.elementAt(index);
		if (b != null) {
			graphics.drawBitmap(5, y+((this.getRowHeight()-14)>>1), 14, 14, unreadIcon, 0, 0);		
			//graphics.drawText(b.name, icon.getWidth()+2, y, DrawStyle.ELLIPSIS, width-icon.getWidth()-2);
			graphics.drawText(b.name, 25, y+((this.getRowHeight()-graphics.getFont().getHeight())>>1), DrawStyle.ELLIPSIS, width-25);		
			String message = (String) recentChatHashMap.get(b.jid.trim()); 
			int x = (graphics.getFont().getAdvance(b.name)) + 30;
			graphics.drawText(message, x, y+((this.getRowHeight()-graphics.getFont().getHeight())>>1), DrawStyle.ELLIPSIS, (width-x));		
			graphics.drawLine(0, y, width, y);
		}		
	}


	public Hashtable getRecentChatHashMap() {
		return recentChatHashMap;
	}
}
