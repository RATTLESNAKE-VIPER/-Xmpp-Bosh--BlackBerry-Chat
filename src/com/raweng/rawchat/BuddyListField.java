package com.raweng.rawchat;

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


public class BuddyListField extends ObjectListField {
	public static ChatManager chatManager;
	
	public static final Bitmap offlineIcon = Bitmap.getBitmapResource("offline.png");
	public static final Bitmap awayIcon = Bitmap.getBitmapResource("away.png");
	public static final Bitmap busyIcon = Bitmap.getBitmapResource("busy.png");
	public static final Bitmap onlineIcon = Bitmap.getBitmapResource("online.png");
	public static final Bitmap defaultProfileIcon = Bitmap.getBitmapResource("missing_profile.png");
	
	public static final Bitmap[] statusIcon = new Bitmap[]{offlineIcon, awayIcon, busyIcon, onlineIcon};
	public Vector buddyVector;
	
	
	public BuddyListField() {
		this.buddyVector = new Vector();
		this.setRowHeight(40);
	}
	
	protected boolean navigationClick(int status, int time) {
		int idx = this.getSelectedIndex();
		if (idx >= 0) {
			BuddyListField.chatManager.currentBuddy = (Buddy)buddyVector.elementAt(idx);
			BuddyListField.chatManager.openBuddy(BuddyListField.chatManager.currentBuddy);
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
				BuddyListField.chatManager.currentBuddy = (Buddy)buddyVector.elementAt(idx);
				BuddyListField.chatManager.openBuddy(BuddyListField.chatManager.currentBuddy);
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
		return (Buddy)buddyVector.elementAt(index);		
	}
	
	public int getBuddyIndex(Buddy b) {
		return buddyVector.indexOf(b);
	}	
	
	public void buddyReposition(Buddy b) {
		int index = buddyVector.indexOf(b);
		buddyReposition(index);
	}
	
	public void buddyReposition(int oldIndex) {
		Buddy b = (Buddy)buddyVector.elementAt(oldIndex);
		int newIndex = 0;

		while (newIndex < buddyVector.size() &&
				((b == (Buddy)buddyVector.elementAt(newIndex)) || 
						(b.status < ((Buddy)buddyVector.elementAt(newIndex)).status)))
			++newIndex;

		newIndex = (oldIndex < newIndex) ? (newIndex-1) : newIndex;
		
		if (oldIndex != newIndex) {
			buddyVector.removeElementAt(oldIndex);
			buddyVector.insertElementAt(b, newIndex);
		}
		this.invalidate();
	}
	
	public int findBuddyIndex(String jid) {
		for (int i = buddyVector.size()-1; i >= 0; i--) {
			if (((Buddy)buddyVector.elementAt(i)).jid.equalsIgnoreCase(jid))
				return i;
		}
		
		return -1;
	}
	
	public Buddy findBuddy(String jid) {
		for (int i = buddyVector.size()-1; i >= 0; i--) {
			if (((Buddy)buddyVector.elementAt(i)).jid.equalsIgnoreCase(jid))
				return (Buddy)buddyVector.elementAt(i);
		}
		return null;
	}
	
	public void addBuddy(Buddy b) {
		buddyVector.addElement(b);
		this.insert(buddyVector.indexOf(b));
	}
	
	public boolean deleteBuddy(String jid) {
		int idx;
		for (idx = buddyVector.size()-1; idx >= 0; idx--) {
			if (((Buddy)buddyVector.elementAt(idx)).jid.equalsIgnoreCase(jid))
				break;
		}
		
		if (idx >= 0) {
			buddyVector.removeElementAt(idx);
			this.delete(idx);
			this.getScreen().invalidate();
			return true;
		} else {
			return false;
		}
	}
	
	public void clearBuddies() {
		if (BuddyListField.chatManager.buddyList != null) {
			int i = buddyVector.size();
			while (i-- > 0)
				this.delete(0);
			ChatManager.buddyscreen.delete(BuddyListField.chatManager.buddyList);
			BuddyListField.chatManager.buddyList = null;
		}
	}
	
	public void invalBuddies() {
		if (BuddyListField.chatManager.buddyList != null) {
			for (int i = buddyVector.size()-1; i >= 0; i--) {
				Buddy b = (Buddy) buddyVector.elementAt(i);
				b.status = Buddy.STATUS_OFFLINE;
			}
			
			this.invalidate();
		}
	}
	
	
	
	public void drawListRow(ListField listField, Graphics graphics, int index, int y, int width) {
		// Row styling
		{
			if (graphics.isDrawingStyleSet(Graphics.DRAWSTYLE_FOCUS)) {
				graphics.setColor(0x00DBEDF5);
				graphics.fillRect(0, y, width, listField.getRowHeight());
			}
			graphics.setColor(Color.GRAY);	
			graphics.setFont(Font.getDefault().derive(Font.PLAIN, Font.getDefault().getHeight()-2));
		}		
		
		// NOTICE 14 would be consist the icon size
		Buddy b = (Buddy)buddyVector.elementAt(index);
		if (b != null) {
			if (b.buddyImage == null) {
				graphics.drawBitmap(5, y+((this.getRowHeight()-40)>>1), 40, 40, defaultProfileIcon, 0, 0);
			} else {
				graphics.drawBitmap(5, y+((this.getRowHeight()-40)>>1), 40, 40, b.buddyImage, 0, 0);
			}		
			graphics.drawBitmap(48, y+((this.getRowHeight()-14)>>1), 14, 14, statusIcon[b.status], 0, 0);
			graphics.drawText(b.name, 65, y+((this.getRowHeight()-graphics.getFont().getHeight())>>1), DrawStyle.ELLIPSIS, width-65);
			graphics.drawLine(0, y, width, y);
		}		
	}
}
