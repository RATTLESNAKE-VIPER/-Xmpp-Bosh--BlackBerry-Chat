package com.raweng.bchat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import net.rim.device.api.io.Base64InputStream;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.ui.component.Dialog;

import com.raweng.ui.IconLabelField;
import com.raweng.xmppservice.ChatHistory;
import com.raweng.xmppservice.Connection;
import com.raweng.xmppservice.ThreadEvent;
import com.raweng.xmppservice.XMPPThread;
import com.raweng.xmppservice.XmppListener;

public class ChatManager implements XmppListener {
	public static BChat bchat;
	private Connection connection;
	private XMPPThread thread;
	
	public int retryCount;
	
	public BuddyListField buddyList;
	public static BuddyScreen buddyscreen;	
	public Buddy currentBuddy;
	
	private ChatHistory chatHistory;
	
	// status values
	public int state;	
	public final static int STATE_STARTUP = 0x0;
	public final static int STATE_LOGINING = 0x1;
	public final static int STATE_ONLINE = 0x2;
	public final static int STATE_FAILED = 0x3;
	public final static int STATE_WAITING = 0x4;
	public final static int STATE_RETRYING = 0x5;
	
	
	final ThreadEvent threadEvent = new ThreadEvent();
	private int buddyVCardCounter = 0;
	
	

	public ChatManager(Connection connection) {
		this.connection = connection;
		this.state = STATE_STARTUP;
		this.retryCount = 0;
		
		this.chatHistory = new ChatHistory();
		Buddy.chatHistory = this.chatHistory;
		MessageScreen.chatHistory = this.chatHistory;
			
		RecentBuddyListField.chatHandler = this;
		BuddyListField.chatManager = this;
		BuddyScreen.chatManager = this;
		MessageScreen.chatManager = this;
	}	
	
	
	public void login(final String username, final String password, final String domain, ServerModel serverDef, int networkType) {
		if (this.state != STATE_LOGINING) {
			this.state = STATE_LOGINING;
			
			this.connection.setUsername(username);
			this.connection.setPassword(password);
			this.connection.setHost(domain);
			this.connection.setMyjid(username + "@" + domain);
			this.connection.setServer(serverDef.server);
			this.connection.setPort(serverDef.port);
			this.connection.setHttpburl(serverDef.boshUrl);
			this.connection.setBosh(serverDef.useBosh);
			this.connection.setNetworkType(networkType);
			this.connection.setSSL(serverDef.usessl);
			
			this.connection.connect(serverDef.useBosh);	
			this.thread = (XMPPThread) this.connection.getThread();
			this.connection.addListener(this);
		}
	}
	
	
	public void onAuth(String resource) {
		bchat.invokeAndWait(new Runnable() {
			public void run() {
				onAuthHandler();
			}
		});
		try {
			this.thread.getRoster();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void onAuthFailed(final String message) {
		this.connection.removeListener(this);
		this.thread = null;	
		this.buddyVCardCounter = 0;
		bchat.invokeAndWait(new Runnable() {
			public void run() {
				onAuthFailedHandler(message);
				Dialog.alert("Login failed.\nAuthentication failure");
			}
		});
	}
	
	
	public void onConnFailed(final String msg) {
		this.connection.removeListener(this);
		this.thread = null;	
		this.buddyVCardCounter = 0;
		bchat.invokeAndWait(new Runnable() {
			public void run() {
				onConnFailedHandler(msg);
			}
		});
	}
	
	
	public void onContactEvent(final String jid, final String name, final String group, final String subscription) {
		bchat.invokeLater(new Runnable() {
			public void run() {
				onContactHandler(jid, name, group, subscription);
			}
		});
	}
	
	
	
	//-------------------------------------------------------VCARD--------------------------------------------------------
	public class ImageRunnable implements Runnable {
		private Vector buddies;
		boolean isFirst = true;
		
		public ImageRunnable(Vector buddies) {
			this.buddies = buddies;
			
		}
		
		public void run() {	
			for (; buddyVCardCounter < buddies.size(); buddyVCardCounter++) {
				Buddy b = (Buddy) buddies.elementAt(buddyVCardCounter);
				try {
					thread.getRosterVCard(b.jid);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					threadEvent.await();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}		
		}		
	}

	
	public void onContactOverEvent() {
		// Get buddies images
		Thread imageThread = new Thread(new ImageRunnable(buddyscreen.buddyList.buddyVector));	
		imageThread.start();	
	}
	
	
	public void onRosterVCardEvent(final String jid, final String base64StringValue) {
		threadEvent.signal();
		
		
		if (jid != null && base64StringValue != null) {
			int idx = jid.indexOf('/');
			final String id;
			// in some instance, jid contains no '/', fix this
			if (idx == -1) {
				id = new String(jid);
			} else {
				id = jid.substring(0, jid.indexOf('/'));
			}
			
			Bitmap offenderImage = null;
			try {
				byte[] imageByte = base64StringValue.getBytes();
				byte[] bs = Base64InputStream.decode(imageByte, 0, imageByte.length);
				offenderImage = Bitmap.createBitmapFromBytes(bs, 0, bs.length, 2);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			final int buddyIdx = buddyList.findBuddyIndex(id);
			if (buddyIdx != -1) {
				final Buddy b = buddyList.getBuddyAt(buddyIdx);
				b.buddyImage = offenderImage;
				buddyList.invalidate(buddyIdx);
				
			} else {
				System.out.println("[warning] Message from unkown buddy");
			}
		}		
	}
	//-------------------------------------------------------VCARD--------------------------------------------------------
	
	
	public void onStatusEvent(final String jid, final String show, final String status) {
		int idx = jid.indexOf('/');
		final String id;
		// in some instance, jid contains no '/', fix this
		if (idx == -1) {
			id = new String(jid);
		} else {
			id = jid.substring(0, jid.indexOf('/'));
		}
		bchat.invokeLater(new Runnable() {
			public void run() {
				onStatusHandler(id, show, status);
			}
		});		
	}	
	
	
	public void onMessageEvent(final String from, final String to, final String body, final String id) {
		if (body.length() == 0)
			return;
		
		int idx = from.indexOf('/');
		final String fromjid;
		// in some instance, jid contains no '/', fix this
		if (idx == -1) {
			fromjid = new String(from);
		} else {
			fromjid = from.substring(0, from.indexOf('/'));
		}
		
		int index = to.indexOf('/');
		final String tojid;
		// in some instance, jid contains no '/', fix this
		if (index == -1) {
			tojid = new String(to);
		} else {
			tojid = to.substring(0, to.indexOf('/'));
		}
		
		bchat.invokeLater(new Runnable() {
			public void run() {				
				onMessageHandler(fromjid, tojid, body, id);
			}
		});
	}
	
	
	public void onSubscribeEvent(final String jid) {		
		bchat.invokeLater(new Runnable() {
			public void run() {
				onSubscribeHandler(jid);
			}
		});	
	}

	public void onUnsubscribeEvent(final String jid) {
		bchat.invokeLater(new Runnable() {
			public void run() {
				Dialog.inform(jid + " has removed you from his/her buddy list");
				buddyList.deleteBuddy(jid);
			}
		});
		this.thread.unsubscribe(jid);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/*
	 * Only called by MessageTextField and currentBuddy mustn't be null
	 */
	public void sendMessage(final String msg) {
		(new Thread() {
			public void run() {
				thread.sendMessage(currentBuddy.jid, msg, currentBuddy.session);
			}
		}).start();
		currentBuddy.sendMessage(this.connection.getMyjid(), currentBuddy.jid, this.connection.getUsername(), msg);
		
		
		// Add to recent chat list
		if (buddyscreen.getRecentBuddyList().getRecentChatHashMap().get(currentBuddy.jid) == null) {
			buddyscreen.getRecentBuddyList().getRecentChatHashMap().put(currentBuddy.jid.trim(), msg);
			if (buddyscreen.getField(0) != buddyscreen.getRecentBuddyList()) {
				buddyscreen.insert(buddyscreen.getRecentBuddyList(), 0);
				buddyscreen.getRecentBuddyList().setFocus();
			}
			buddyscreen.getRecentBuddyList().addBuddy(currentBuddy);
		} else {
			buddyscreen.getRecentBuddyList().getRecentChatHashMap().put(currentBuddy.jid, msg);
			buddyscreen.getRecentBuddyList().invalidate();
		}
	}
	
	public void openBuddy(Buddy b) {
		if (currentBuddy != null && currentBuddy.getMsgScreen().isDisplayed()) {
			bchat.popScreen(currentBuddy.getMsgScreen());
		}
		
		this.currentBuddy = b;
		bchat.pushScreen(b.getMsgScreen());
	}
	
	public void switchBuddy(Buddy b) {
		if (currentBuddy == b) {
			return;
		}
		else if (currentBuddy != null && currentBuddy.getMsgScreen().isDisplayed()) {
			bchat.popScreen(currentBuddy.getMsgScreen());
			currentBuddy = null;
		}
		
		this.openBuddy(b);
	}
	
	
	
	public void setMyStatus(int s, boolean customText, String text) {
		switch (s) {
		case STATE_ONLINE:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.onlineIcon, "Available");
			buddyscreen.setTitle(buddyscreen.statusBanner);
			break;
		
		case STATE_WAITING:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.offlineIcon, "Waiting to reconnect..." + String.valueOf(AppSavedData.retryDelay) + "s...");
			buddyscreen.setTitle(buddyscreen.statusBanner);
			break;
			
		case STATE_RETRYING:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.offlineIcon, "Reconnecting...");
			buddyscreen.setTitle(buddyscreen.statusBanner);
			break;
			
		case STATE_FAILED:
			buddyscreen.statusBanner = new IconLabelField(BuddyListField.offlineIcon, "Offline");
			buddyscreen.setTitle(buddyscreen.statusBanner);
			break;
			
		default:
			System.out.println("Unhandled state value" + String.valueOf(s));
			break;
		}
		
		if (customText)
			buddyscreen.statusBanner.setText(text);
		
	}
	
	
	public void subscribe(final String jid) {
		(new Thread() {
			public void run() {
				thread.subscribe(jid);
			}
		}).start();
	}
	
	public void unsubscribe(final String jid) {
		(new Thread() {
			public void run() {
				thread.unsubscribe(jid);
			}
		}).start();
	}
	
	public void retryBChat() {			
		this.connection.connect(this.connection.isBosh());
		this.thread = (XMPPThread) this.connection.getThread();
		this.connection.addListener(this);
	}
	
	public void logoff() {
		this.chatHistory.deleteChatHistory();
		this.connection.removeListener(this);
		this.buddyVCardCounter = 0;
		
		(new Thread() {
			public void run() {
				thread.logoff();
			}
		}).start();			
	}
	
	public void exitBtalk() {
		System.exit(0);
	}
	
	
	
	
	
	
	
	private void onAuthHandler() {
		if (this.state == STATE_LOGINING) {
			AppSavedData.setUserInfo(connection.getMyjid(), connection.getPassword(), connection.getServer(), connection.getPort(), connection.isBosh(), connection.getHttpburl(), connection.isSSL(), connection.getNetworkType());
			
			Screen screen = bchat.getActiveScreen();
			if (screen instanceof LoginScreen) {
				LoginScreen loginScreen = (LoginScreen) screen;
				buddyList = new BuddyListField();
				buddyscreen = new BuddyScreen(buddyList);
				bchat.popScreen(loginScreen);
				bchat.pushScreen(buddyscreen);
				loginScreen = null;
				
			} else if (screen instanceof BuddyScreen) {
				buddyscreen = (BuddyScreen) screen;
				buddyList = buddyscreen.getBuddyList();
			}
			
		} else if (this.state == STATE_RETRYING) {
			this.setMyStatus(STATE_ONLINE, false, null);
		}
		this.state = STATE_ONLINE;
	}

	
	private void onAuthFailedHandler(final String msg) {
		if (BChat.DEBUG) {
			BChat.debugConsole.addDebugMsg("onAuthFailedHandler err: " + msg);
		}
		this.state = STATE_FAILED;		
	}
	
	
	private void onConnFailedHandler(final String msg) {
		if (BChat.DEBUG) {
			BChat.debugConsole.addDebugMsg("onConnFailedHandler err: " + msg);
		}
		
		if (this.state == STATE_ONLINE || this.state == STATE_RETRYING) {
			buddyList.invalBuddies();
			
			if (AppSavedData.autoRetry) {
				this.retryCount++;
				if (AppSavedData.retryLimit > 0 && this.retryCount > AppSavedData.retryLimit) {
					this.setMyStatus(STATE_FAILED, false, null);
					this.state = STATE_FAILED;
					Dialog.alert("Retry count exceeded");
					return;
				}
				
				this.setMyStatus(STATE_WAITING, false, null);
				this.state = STATE_WAITING;
				TimerTask retryTask = new TimerTask() {
					public void run() {
						state = STATE_RETRYING;
						bchat.invokeAndWait(new Runnable() {
							public void run() {
								setMyStatus(STATE_RETRYING, false, null);
							}
						});
						retryBChat();
					}
				};
				
				Timer retrytimer = new Timer();
				retrytimer.schedule(retryTask, AppSavedData.retryDelay * 1000);
				return;
			} else {
				this.state = STATE_FAILED;
			}
				
		} else if (this.state == STATE_LOGINING) {
			this.state = STATE_FAILED;
					
		}
		Dialog.alert("Connection failed, please retry" + "\nInfo: "+msg);
	}

	
	
	private void onContactHandler(final String jid, final String name, final String group, final String subscription) {
		if (subscription.equals("both") && buddyList.findBuddyIndex(jid) == -1) {
			buddyList.addBuddy(new Buddy(jid, name, Buddy.STATUS_OFFLINE));
		}
	}

	
	private void onStatusHandler(final String jid, final String show, final String status) {
		int idx = buddyList.findBuddyIndex(jid);
		Buddy b;
		
		if (idx != -1) {
			b = (Buddy)buddyList.buddyVector.elementAt(idx);
			int state = 0;
			if (show.equals(""))
				state = Buddy.STATUS_ONLINE;
			else if (show.equals("chat"))
				state = Buddy.STATUS_ONLINE;
			else if (show.equals("away"))
				state = Buddy.STATUS_AWAY;
			else if (show.equals("xa"))
				state = Buddy.STATUS_AWAY;
			else if (show.equals("dnd"))
				state = Buddy.STATUS_BUSY;
			else if (show.equals("na"))
				state = Buddy.STATUS_OFFLINE;
			else
				System.out.println("Unhandled status: "+jid+" " + show+ " "+status);
			
			b.custom_str = status;
			if (b.status == state)
				return;
			else {
				b.status = state;
				buddyList.buddyReposition(idx);
			}
		} else {
			System.out.println("No buddy matches: "+jid+" " + show+ " "+status);
		}
	}
	
	
	private void onMessageHandler(final String from, final String to, final String body, final String id) {
		boolean isCurrentBuddy;
		if (currentBuddy != null && currentBuddy.jid.equalsIgnoreCase(from)) {
			isCurrentBuddy = true;
			currentBuddy.session = id;
			currentBuddy.receiveMessage(from, to, buddyscreen.getBuddyList().findBuddy(from).name, body, true);
			
		// from other buddy
		} else {
			isCurrentBuddy = false;
			final int idx = buddyList.findBuddyIndex(from);
			if (idx != -1) {
				final Buddy b = buddyList.getBuddyAt(idx);
				
				// Add to recent chat list
				if (buddyscreen.getRecentBuddyList().getRecentChatHashMap().get(from) == null) {
					buddyscreen.getRecentBuddyList().getRecentChatHashMap().put(from.trim(), body);
					if (buddyscreen.getField(0) != buddyscreen.getRecentBuddyList()) {
						buddyscreen.insert(buddyscreen.getRecentBuddyList(), 0);
						buddyscreen.getRecentBuddyList().setFocus();
					}
					buddyscreen.getRecentBuddyList().addBuddy(b);
				} else {
					buddyscreen.getRecentBuddyList().getRecentChatHashMap().put(from.trim(), body);
					buddyscreen.getRecentBuddyList().invalidate();
				}
				
				// Notification
				if (currentBuddy != null && !currentBuddy.jid.equalsIgnoreCase(from)) {
					currentBuddy.newMessage(from, to, buddyscreen.getBuddyList().findBuddy(from).name, body, true);
				} else {
					b.newMessage(from, to, buddyscreen.getBuddyList().findBuddy(from).name, body, false);
				}
			} else {
				System.out.println("[warning] Message from unkown buddy");
			}
		}
	}
	
	
	private void onSubscribeHandler(final String jid) {
		int rst = Dialog.ask("\""+jid+"\""+"request to add you as buddy.",
				new String[] {"Accept", "Deny", "Later"},
				new int[] {1, 2, 3}, 1);
		
		switch (rst) {
		case 1:
			(new Thread() {
				public void run() {
					thread.subscribed(jid);
					thread.subscribe(jid);
				}
			}).start();
			return;
		case 2:
			(new Thread() {
				public void run() {
					thread.unsubscribed(jid);
				}
			}).start();
			return;
		case 3:
			return;
		}
	}
}
