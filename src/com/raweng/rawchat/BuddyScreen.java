package com.raweng.rawchat;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.DialogClosedListener;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.container.MainScreen;

import com.raweng.ui.IconLabelField;
import com.raweng.xmppservice.Connection;


public class BuddyScreen extends MainScreen {
	public static BChat btalk;
	public static ChatManager chatManager;
	
	public IconLabelField statusBanner;	
	private RecentBuddyListField recentBuddyList;
	public BuddyListField buddyList;
	
	
	public BuddyScreen(BuddyListField l) {
		statusBanner = new IconLabelField(BuddyListField.onlineIcon, "Available");
		this.setTitle(statusBanner);
		
		recentBuddyList = new RecentBuddyListField();
		this.buddyList = l;
		
		//this.add(recentBuddyList);
		this.add(l);
		
		this.addMenuItem(new MenuItem("Chat", 0, 0) {
			public void run() {
				Field focusField = getLeafFieldWithFocus();
				if (focusField != null) {
					if (focusField instanceof RecentBuddyListField) {
						int idx = recentBuddyList.getSelectedIndex();
						if (idx >= 0) {
							BuddyScreen.chatManager.currentBuddy = (Buddy) recentBuddyList.recentBuddyVector.elementAt(idx);
							BuddyScreen.chatManager.openBuddy(BuddyScreen.chatManager.currentBuddy);
						}
						
					} else if (focusField instanceof BuddyListField) {
						int idx = buddyList.getSelectedIndex();
						if (idx >= 0) {
							BuddyScreen.chatManager.currentBuddy = (Buddy) buddyList.buddyVector.elementAt(idx);
							BuddyScreen.chatManager.openBuddy(BuddyScreen.chatManager.currentBuddy);
						}
					}
				}				
			}
		});
		
		this.addMenuItem(new MenuItem("Retry", 1, 0) {
			public void run() {
				if (BuddyScreen.chatManager.state == ChatManager.STATE_FAILED) {
					BuddyScreen.chatManager.state = ChatManager.STATE_RETRYING;
					BuddyScreen.chatManager.setMyStatus(ChatManager.STATE_RETRYING, false, null);
					BuddyScreen.chatManager.retryCount = 0;
					BuddyScreen.chatManager.retryBChat();
				}
			}
		});
		
		this.addMenuItem(new MenuItem("Buddy info", 0x00020010, 0) {
			// NOTICE: this array use status as index
			final String[] STATUS_STR = new String[] {"Offline", "Away", "Busy", "Online"};
			public void run() {
				Buddy b = null;
				
				Field focusField = getLeafFieldWithFocus();
				if (focusField != null) {
					if (focusField instanceof RecentBuddyListField) {
						if (recentBuddyList.recentBuddyVector.size() <= 0)
							return;
						b = recentBuddyList.getBuddyAt(recentBuddyList.getSelectedIndex());
						
					} else if (focusField instanceof BuddyListField) {
						if (buddyList.buddyVector.size() <= 0)
							return;
						b = buddyList.getBuddyAt(buddyList.getSelectedIndex());
					}
				}
				
				if (b == null)
					return;
				if (b.name.equalsIgnoreCase(b.jid)) {
					Dialog buddyInfoDialog = new Dialog(Dialog.D_OK, "ID: "+ b.jid+"\n"+STATUS_STR[b.status]+"\n"+b.custom_str,
							0, null, Screen.LEFTMOST);
					buddyInfoDialog.setEscapeEnabled(true);
					buddyInfoDialog.show();
				} else {
					Dialog buddyInfoDialog = new Dialog(Dialog.D_OK, "Name: "+b.name+"\nID: "+b.jid+"\n"+STATUS_STR[b.status]+"\n"+b.custom_str,
							0, null, Screen.LEFTMOST);
					buddyInfoDialog.setEscapeEnabled(true);
					buddyInfoDialog.show();
				}
			}
		});
		
		this.addMenuItem(new MenuItem("New buddy", 0x00020011, 0) {
			public void run() {
				// TODO finish add new buddy
				final EditField jidField;
				Dialog addBuddyDialog = new Dialog(Dialog.D_OK_CANCEL, "Buddy ID (example@gmail.com)", 0, null, Manager.USE_ALL_WIDTH);
				jidField = new EditField(EditField.NO_COMPLEX_INPUT | EditField.NO_NEWLINE);
				addBuddyDialog.add(jidField);
				addBuddyDialog.setDialogClosedListener(new DialogClosedListener() {
					public void dialogClosed(Dialog dialog, int choice) {
						switch (choice) {
						case 0:
							final String jid = jidField.getText();
							if (jid.indexOf('@') == -1) {
								Dialog.alert("Not a legal Email address!");
							} else {
								(new Thread() {
									public void run() {
										BuddyScreen.chatManager.subscribe(jid);
									}
								}).start();
							}
							return;
						case -1:
							return;
						default:
							return;
						}
					}
				});
				addBuddyDialog.show();
			}
			
		});
		
		this.addMenuItem(new MenuItem("Delete buddy", 0x00020012, 0) {
			public void run() {
				//TODO finish delete buddy
				if (buddyList.buddyVector.size() <= 0)
					return;
				final Buddy b = buddyList.getBuddyAt(buddyList.getSelectedIndex());
				String str;
				if (!b.name.equals(b.jid))
					str = b.name+"("+b.jid+")";
				else
					str = b.jid;
				int rst = Dialog.ask("Delete buddy"+" \""+str+"\"?", new String[] {"Yes", "No"}, new int[] {1, 2}, 2);
				
				switch (rst) {
				case 1:
					(new Thread() {
						public void run() {
							BuddyScreen.chatManager.unsubscribe(b.jid);
						}
					}).start();
					buddyList.deleteBuddy(b.jid);
					
					// Remove from recent list if present
					ChatManager.buddyscreen.getRecentBuddyList().getRecentChatHashMap().remove(b.jid.trim());
					ChatManager.buddyscreen.getRecentBuddyList().deleteBuddy(b.jid.trim());
					if (ChatManager.buddyscreen.getRecentBuddyList().recentBuddyVector.size() == 0)  {
						ChatManager.buddyscreen.delete(ChatManager.buddyscreen.getRecentBuddyList());
					}
					
					return;
				case 2:
					return;
				}
			}
		});
		
		this.addMenuItem(new MenuItem("Logout", 0x00030005, 0) {
			public void run() {
				chatManager.logoff();
				AppSavedData.destroyUserInfo();
				btalk.popScreen(BuddyScreen.this);
				btalk.pushScreen(new LoginScreen(Connection.getInstance(), true));
			}
		});
		
		// add exit menuitem
		this.addMenuItem(new MenuItem("Exit", 0x00030010, 0) {
			public void run() {
				if (BuddyScreen.chatManager.state == ChatManager.STATE_ONLINE) {
					BuddyScreen.chatManager.logoff();
				}
				BuddyScreen.chatManager.exitBtalk();
			}
		});
		if (BChat.DEBUG) {
			this.addMenuItem(new MenuItem("Debug console", 0x00030006, 0) {
				public void run() {
					btalk.pushScreen(BChat.debugConsole);
				}
			});
		}
	}
	
	public boolean onClose() {
		if (BuddyScreen.chatManager.state == ChatManager.STATE_ONLINE ||
				BuddyScreen.chatManager.state == ChatManager.STATE_RETRYING ||
				BuddyScreen.chatManager.state == ChatManager.STATE_WAITING) {
			btalk.requestBackground();
		} else {
			if (BuddyScreen.chatManager.state == ChatManager.STATE_ONLINE) {
				BuddyScreen.chatManager.logoff();
			}
			BuddyScreen.chatManager.exitBtalk();
		}
		return true;
	}
	
	public boolean pageDown(int amount, int status, int time) {
		return this.trackwheelRoll(amount, status, time);
	}
	
	public boolean pageUp(int amount, int status, int time) {
		return this.trackwheelRoll(amount, status, time);
	}

	public BuddyListField getBuddyList() {
		return buddyList;
	}

	public RecentBuddyListField getRecentBuddyList() {
		return recentBuddyList;
	}
}
