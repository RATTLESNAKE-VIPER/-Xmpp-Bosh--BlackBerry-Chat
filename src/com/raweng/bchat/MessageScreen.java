package com.raweng.bchat;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStoreException;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.XYEdges;
import net.rim.device.api.ui.component.BitmapField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.NullField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

import com.raweng.ui.ChatBoxTextBoxField;
import com.raweng.ui.ChatBubble;
import com.raweng.xmppservice.ChatHistory;
import com.raweng.xmppservice.Connection;


public class MessageScreen extends MainScreen {
	public static ChatManager chatManager;
	public static ChatHistory chatHistory;	
	private final Buddy buddy;

	private VerticalFieldManager bubbleManager;
	private NewMessageNotificationManager newMessageNotificationManager;
	
	private XYEdges bubbleMargins = new XYEdges(0, 5, 0, 5);

	//Get the device width and height  
	private final int width = Display.getWidth();  
	private final int height = Display.getHeight();
	private int titleHeight;
	
	private HorizontalFieldManager rootManager;
	private HorizontalFieldManager footer;
	private ChatBoxTextBoxField ef;
	private NullField nullField;
	


	public MessageScreen(Buddy b) {
		super(NO_VERTICAL_SCROLL | NO_VERTICAL_SCROLLBAR);
		this.buddy = b;
		this.setTitle("Chat with " + b.name);		
		
		Manager contentManager = getMainManager();
    	Manager screenManager = contentManager.getManager();
    	Field titleField = screenManager.getField(0);
    	titleHeight = titleField.getPreferredHeight();
    	

		this.initUI();
		
		
		this.addMenuItem(new MenuItem("Remove from Recent List", 0, 0) {
			public void run() {
				ChatManager.buddyscreen.getRecentBuddyList().getRecentChatHashMap().remove(MessageScreen.this.buddy.jid.trim());
				ChatManager.buddyscreen.getRecentBuddyList().deleteBuddy(MessageScreen.this.buddy.jid.trim());
				if (ChatManager.buddyscreen.getRecentBuddyList().recentBuddyVector.size() == 0)  {
					ChatManager.buddyscreen.delete(ChatManager.buddyscreen.getRecentBuddyList());
				}
			}
		});
	}


	private void initUI() {		
		//Draw background gradient on this manager and add VerticalFieldManager for scrolling.
		rootManager = new HorizontalFieldManager() {
			public void paint(Graphics g)
			{
				int saveAlpha = g.getGlobalAlpha();
		        g.setGlobalAlpha(200);		        
		        
				//Variables for drawing the gradient
				int[] X_PTS_MAIN = { 0, width, width, 0}; 
				int[] Y_PTS_MAIN = { 0, 0, height, height }; 
				int[] drawColors_MAIN = { 0x00EAEAEA, 0x00EAEAEA, 0x00C8C8C8, 0x00C8C8C8 };
				//int[] drawColors_MAIN = { 0x00EAEAEA, 0x00EAEAEA, 0x00C7C7C7, 0x00C7C7C7 };

				try {
					//Draw the gradients   
					g.drawShadedFilledPath(X_PTS_MAIN, Y_PTS_MAIN, null, drawColors_MAIN, null);

				} catch (IllegalArgumentException iae) {
					System.out.println("Bad arguments."); 
				}

				g.setGlobalAlpha(saveAlpha);
				
				//Call super to paint the graphics on the inherited window 
				super.paint(g);
			}
		};
		bubbleManager = new VerticalFieldManager( USE_ALL_WIDTH | USE_ALL_HEIGHT | VERTICAL_SCROLL /*| VERTICAL_SCROLLBAR*/ );
		rootManager.add(bubbleManager);
		add(rootManager);
		
		nullField = new NullField() {
			protected void layout(int width, int height) {
				super.layout(width, height);
				super.setExtent(width, 0);
			}
		};
		
		
		



		
		footer = new HorizontalFieldManager(USE_ALL_WIDTH) {
			public void paint(Graphics g)
			{	
				int saveColor = g.getColor();
				int saveAlpha = g.getGlobalAlpha();				
				
		        g.setGlobalAlpha(240);		        
				final Bitmap gradientBitmap1 = getGradientBitmap(Display.getWidth(), getHeight()/2, 0x00505050, 0x00252525);
				final Bitmap gradientBitmap2 = getGradientBitmap(Display.getWidth(), getHeight()/2, 0x000C0C0C, 0x00010101);
				g.drawBitmap(0, 0, Display.getWidth(), getHeight()/2, gradientBitmap1, 0, 0);
				g.drawBitmap(0,  getHeight()/2, Display.getWidth(), getHeight()/2, gradientBitmap2, 0, 0);
				g.setGlobalAlpha(saveAlpha);				
				
				g.setColor(0x001C1C1C);
				g.drawLine(0, 0, getWidth(), 0);
				g.setColor(Color.GRAY);
				g.drawLine(0, 1, getWidth(), 1);
				g.drawLine(0, 2, getWidth(), 2);				
				
				g.setColor(saveColor);				
				//Call super to paint the graphics on the inherited window 
				super.paint(g);
			}
		};
		
		ef = new ChatBoxTextBoxField(0, 30, Field.FIELD_VCENTER) {
			protected boolean navigationClick(int status, int time) {
				send();
				return true;
			}
		};
		ef.setFont(Font.getDefault().derive(Font.PLAIN, 14));	
		ef.setMargin(10, 5, 10, 10);
		
		footer.add(ef);
		setStatus(footer);
	}


	/*public boolean keyChar(char key, int status, int time) {
		Field currField = this.getFieldWithFocus();
		if (currField == ef) {
			//CustomTextBox was basically my EditField, just made a custom field for it.
			return super.keyChar(key, status, time);
		} else {
			switch (key) {			
			case Characters.ENTER:
				this.send();
				return true;				
			default:
				return super.keyChar(key, status, time);
			}
		}
	}*/


	private void send() {
		if (ef.getText().trim().length() > 0) {		
			UiApplication.getUiApplication().invokeLater(new Runnable() {				
				public void run() {	
					if (chatManager.state == ChatManager.STATE_ONLINE) {
						chatManager.sendMessage(ef.getText().trim());
						ef.setText("");
					} else {
						Dialog.alert("You are currently not online!");
					}
				}
			});	
		}
	}








	// needn't check whether this is the current buddy
	public void sendMessage(String from, String to, String msg, boolean showTitle, String time) {
		HorizontalFieldManager chatContainer = new HorizontalFieldManager(USE_ALL_WIDTH);
		chatContainer.setMargin(10, 10, 5, 10);
		//bubbleManager.add(chatContainer);
		bubbleManager.insert(chatContainer, bubbleManager.getFieldCount()-1);
		
		
		BitmapField buddyBitmapField = new BitmapField(BuddyListField.defaultProfileIcon) {
			protected void layout(int width, int height) {
				super.layout(width, height);
				super.setExtent(40, 40);
			}
		};
		chatContainer.add(buddyBitmapField);		
		
		MessageModel messageModel = new MessageModel(MessageModel.TYPE_OUT, from, msg);
		long outMessageStyle = Field.FIELD_LEFT;
		ChatBubble bbl = new ChatBubble(messageModel, outMessageStyle);
		bbl.setMargin(bubbleMargins);
		chatContainer.add(bbl);		

		
		this.scrollToBottom();
		ef.setFocus();
	}

	public void receiveMessage(String from, String to, String msg, boolean current, boolean showTitle, String time) {
		HorizontalFieldManager chatContainer = new HorizontalFieldManager(USE_ALL_WIDTH);
		chatContainer.setMargin(10, 10, 5, 10);
		//bubbleManager.add(chatContainer);
		bubbleManager.insert(chatContainer, bubbleManager.getFieldCount()-1);
		
		
		Buddy fromBuddy = ChatManager.buddyscreen.buddyList.findBuddy(from);		
		if (fromBuddy != null && fromBuddy.buddyImage != null) {
			BitmapField buddyBitmapField = new BitmapField(fromBuddy.buddyImage) {
				protected void layout(int width, int height) {
					super.layout(width, height);
					super.setExtent(40, 40);
				}
			};
			chatContainer.add(buddyBitmapField);
		} else {
			BitmapField buddyBitmapField = new BitmapField(BuddyListField.defaultProfileIcon) {
				protected void layout(int width, int height) {
					super.layout(width, height);
					super.setExtent(40, 40);
				}
			};
			chatContainer.add(buddyBitmapField);
		}
		
		MessageModel messageModel = new MessageModel(MessageModel.TYPE_IN, from, msg);
		long inMessageStyle = Field.FIELD_RIGHT;
		ChatBubble bbl = new ChatBubble(messageModel, inMessageStyle);
		bbl.setMargin(bubbleMargins);
		chatContainer.add(bbl);
		

		this.scrollToBottom();
		if (current) {
			ef.setFocus();
		}
	}	

	public void newMessage(final String from, final String body, final long time) {
		UiApplication.getUiApplication().invokeLater(new Runnable() {				
			public void run() {
				newMessageNotificationManager.newMessage(from, body);
			}
		});
	}
	
	
	private void scrollToBottom() {
		if (bubbleManager.getFieldCount() > 0) {
			//bubbleManager.getField(bubbleManager.getFieldCount()-1).setFocus();
			
			// [field count - 2] because we have added nullfield to last position 
			Field f = bubbleManager.getField(bubbleManager.getFieldCount()-1);
			int y = f.getTop() - (height - ((footer.getHeight() * 2) + titleHeight + 20));
			if (y > 0) {
				bubbleManager.setVerticalScroll(y);
			}			
		}
	}



	// when pushed into stack
	protected void onDisplay() {
		newMessageNotificationManager = new NewMessageNotificationManager();
		bubbleManager.add(newMessageNotificationManager);
		ef.setFocus();


		RecordEnumeration re = MessageScreen.chatHistory.getChatHistory(this.buddy.jid, Connection.getInstance().getMyjid());
		if (re != null) {
			try {
				while(re.hasNextElement()) {
					int id = re.nextRecordId();
					ByteArrayInputStream bais = new ByteArrayInputStream(MessageScreen.chatHistory.getRecordStore().getRecord(id));
					DataInputStream inputStream = new DataInputStream(bais);
					try {
						long time = inputStream.readLong();	                	
						String fromjid = inputStream.readUTF();
						String tojid = inputStream.readUTF();
						String name = inputStream.readUTF();
						String message = inputStream.readUTF();


						if (fromjid.equals(Connection.getInstance().getMyjid())) {
							HorizontalFieldManager chatContainer = new HorizontalFieldManager(USE_ALL_WIDTH);
							chatContainer.setMargin(10, 10, 5, 10);
							bubbleManager.add(chatContainer);
							
							
							BitmapField buddyBitmapField = new BitmapField(BuddyListField.defaultProfileIcon) {
								protected void layout(int width, int height) {
									super.layout(width, height);
									super.setExtent(40, 40);
								}
							};
							chatContainer.add(buddyBitmapField);							
							
							MessageModel messageModel = new MessageModel(MessageModel.TYPE_OUT, name, message);
							long outMessageStyle = Field.FIELD_LEFT;
							//MessageBubble bbl = new MessageBubble(messageModel, outMessageStyle);
							ChatBubble bbl = new ChatBubble(messageModel, outMessageStyle);
							bbl.setMargin(bubbleMargins);
							chatContainer.add(bbl);
							
						} else {
							HorizontalFieldManager chatContainer = new HorizontalFieldManager(USE_ALL_WIDTH);
							chatContainer.setMargin(10, 10, 5, 10);
							bubbleManager.add(chatContainer);
							
							
							Buddy fromBuddy = ChatManager.buddyscreen.buddyList.findBuddy(fromjid);		
							if (fromBuddy != null && fromBuddy.buddyImage != null) {
								BitmapField buddyBitmapField = new BitmapField(fromBuddy.buddyImage) {
									protected void layout(int width, int height) {
										super.layout(width, height);
										super.setExtent(40, 40);
									}
								};
								chatContainer.add(buddyBitmapField);
							} else {
								BitmapField buddyBitmapField = new BitmapField(BuddyListField.defaultProfileIcon) {
									protected void layout(int width, int height) {
										super.layout(width, height);
										super.setExtent(40, 40);
									}
								};
								chatContainer.add(buddyBitmapField);
							}
							
							MessageModel messageModel = new MessageModel(MessageModel.TYPE_IN, name, message);
							long inMessageStyle = Field.FIELD_RIGHT;
							//MessageBubble bbl = new MessageBubble(messageModel, inMessageStyle);
							ChatBubble bbl = new ChatBubble(messageModel, inMessageStyle);
							bbl.setMargin(bubbleMargins);
							chatContainer.add(bbl);
						}
					}
					catch (EOFException eofe) {
						System.out.println(eofe);
						eofe.printStackTrace();
					}
				}
			}
			catch (RecordStoreException rse) {
				System.out.println(rse);
				rse.printStackTrace();
			}
			catch (IOException ioe) {
				System.out.println(ioe);
				ioe.printStackTrace();
			}
		}
		
		bubbleManager.add(nullField);
		this.scrollToBottom();
		ef.setFocus();
	}

	
	
	
	// when popped off the stack	
	public boolean onClose() {
		newMessageNotificationManager.stopAnimation();
		bubbleManager.delete(newMessageNotificationManager);
		bubbleManager.deleteAll();
		
		if (ChatManager.buddyscreen.getRecentBuddyList().getRecentChatHashMap().get(this.buddy.jid.trim()) != null) {
			ChatManager.buddyscreen.getRecentBuddyList().getRecentChatHashMap().put(this.buddy.jid.trim(), "");
			ChatManager.buddyscreen.getRecentBuddyList().invalidate(ChatManager.buddyscreen.getRecentBuddyList().getSelectedIndex());
		}

		Field focusField = getLeafFieldWithFocus();
		if (focusField != null) {
			if (focusField instanceof RecentBuddyListField) {
				int idx = ChatManager.buddyscreen.getRecentBuddyList().getSelectedIndex();
				if (idx >= 0) {
					BuddyScreen.chatManager.currentBuddy = (Buddy) ChatManager.buddyscreen.getRecentBuddyList().recentBuddyVector.elementAt(idx);
					MessageScreen.chatManager.buddyList.setSelectedIndex(MessageScreen.chatManager.buddyList.getBuddyIndex(buddy));
				}

			} else if (focusField instanceof BuddyListField) {
				int idx = MessageScreen.chatManager.buddyList.getSelectedIndex();
				if (idx >= 0) {
					BuddyScreen.chatManager.currentBuddy = (Buddy) MessageScreen.chatManager.buddyList.buddyVector.elementAt(idx);
					MessageScreen.chatManager.buddyList.setSelectedIndex(MessageScreen.chatManager.buddyList.getBuddyIndex(buddy));
				}
			}
		}	

		MessageScreen.chatManager.currentBuddy = null;
		return super.onClose();
	}
	
	
	protected boolean onSavePrompt() {
		return true;
	}
	
	
	
	public static Bitmap getGradientBitmap(int cols, int rows, int startColor, int endColor) {
		final Bitmap gradientmul = new Bitmap(cols, rows);	// width, height = columns, rows
		int redStart = (startColor & 0x00FF0000) >> 16;
		int greenStart = (startColor & 0x0000FF00) >> 8;
		int blueStart = startColor & 0x000000FF;
		int redFinish = (endColor & 0x00FF0000) >> 16;
		int greenFinish = (endColor & 0x0000FF00) >> 8;
		int blueFinish = endColor & 0x000000FF;
		int[] rgb = new int[cols * rows];
		for (int row = 0; row < rows; ++row) {
			int redComp = ((redFinish - redStart) * row / rows) + redStart;
			int greenComp = ((greenFinish - greenStart) * row / rows) + greenStart;
			int blueComp = ((blueFinish - blueStart) * row / rows) + blueStart;
			int rowColor = 0xFF000000 | (redComp << 16) | (greenComp << 8) | blueComp;
			for (int col = 0; col < cols; ++col) {
				rgb[row * cols + col] = rowColor;
			}
		}		
		gradientmul.setARGB(rgb, 0, cols, 0, 0, cols, rows);
		return gradientmul;
	} 	
}
