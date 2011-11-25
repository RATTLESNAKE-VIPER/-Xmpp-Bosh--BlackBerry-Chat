package com.raweng.ui;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.EditField;

import com.raweng.rawchat.MessageModel;

public class ChatBubble extends Manager {
	private static final Bitmap blue_bubble = Bitmap.getBitmapResource("blue_bubble.png");
	private static final Bitmap blue_left_bar = Bitmap.getBitmapResource("blue_left_bar.png");
	private static final Bitmap blue_top_bar = Bitmap.getBitmapResource("blue_top_bar.png");
	private static final Bitmap blue_right_bar = Bitmap.getBitmapResource("blue_right_bar.png");
	private static final Bitmap blue_bottom_bar = Bitmap.getBitmapResource("blue_bottom_bar.png");

	private static final Bitmap orange_bubble = Bitmap.getBitmapResource("orange_bubble.png");
	private static final Bitmap orange_left_bar = Bitmap.getBitmapResource("orange_left_bar.png");
	private static final Bitmap orange_top_bar = Bitmap.getBitmapResource("orange_top_bar.png");
	private static final Bitmap orange_right_bar = Bitmap.getBitmapResource("orange_right_bar.png");
	private static final Bitmap orange_bottom_bar = Bitmap.getBitmapResource("orange_bottom_bar.png");
	

	private static final int BUBBLE_MARGIN=5;
	//text margins for out bubbles
	private static final int OUT_TEXT_MARGIN_RIGHT=17;	
	private static final int OUT_TEXT_MARGIN_LEFT=17;
	//text margins for in bubbles
	private static final int IN_TEXT_MARGIN_RIGHT=17;
	private static final int IN_TEXT_MARGIN_LEFT=17;
	
	private MessageModel messageModel;
	private EditField buddynameEditField;
	private EditField messageEditField;
	
	
	public ChatBubble(MessageModel msg, long style){
		super(style);
		setMargin(BUBBLE_MARGIN, BUBBLE_MARGIN, BUBBLE_MARGIN, BUBBLE_MARGIN);
		messageModel = msg;
		
		//init the contents of the bubble
		buddynameEditField=new EditField(Field.FIELD_LEFT) {
			protected void paint(Graphics graphics) {
				graphics.setColor(Color.GRAY);
				super.paint(graphics);
			}
		};
		buddynameEditField.setEditable(false);
		buddynameEditField.setFont(Font.getDefault().derive(Font.PLAIN, 13));
		
		messageEditField=new EditField(Field.FIELD_LEFT);
		messageEditField.setEditable(false);
		messageEditField.setFont(Font.getDefault().derive(Font.PLAIN, 14));
		
		
		//establish the margins depending on the direction of the message
		if(messageModel.getType() == MessageModel.TYPE_OUT){
			buddynameEditField.setMargin(10, OUT_TEXT_MARGIN_RIGHT, 2, OUT_TEXT_MARGIN_LEFT);
			messageEditField.setMargin(2, OUT_TEXT_MARGIN_RIGHT, 10, OUT_TEXT_MARGIN_LEFT);			
		}else{
			buddynameEditField.setMargin(10, IN_TEXT_MARGIN_RIGHT, 2, IN_TEXT_MARGIN_LEFT);
			messageEditField.setMargin(2, IN_TEXT_MARGIN_RIGHT, 10, IN_TEXT_MARGIN_LEFT);			
		}
		//add the message and assets to the bubble
		buddynameEditField.setText(messageModel.getBuddyName());
		messageEditField.setText(messageModel.getMessage());
		
		add(buddynameEditField);
		add(messageEditField);
	}
	protected void paintBackground(Graphics g) {
		//paint my bubble
		int col=g.getColor();

		int height = this.getContentHeight();		
		//int width = this.getContentWidth();
		int width = this.getWidth();

		if(messageModel.getType() == MessageModel.TYPE_IN){
			//draw corners
			g.drawBitmap(0, 0, 17, 17, orange_bubble, 0, 0); 					//left top
			g.drawBitmap(width-11, 0, 10, 10, orange_bubble, 36, 0);				//right top
			g.drawBitmap(0, height-11, 17, 11, orange_bubble, 0, 17);			//left bottom
			g.drawBitmap(width-11, height-11, 11, 11, orange_bubble, 36, 17);	//right bottom
			//draw borders
			g.tileRop(Graphics.ROP_SRC_ALPHA, 17, 0, width-27, 9, orange_top_bar, 0, 0);
			g.tileRop(Graphics.ROP_SRC_ALPHA, 7, 17, 10, height-28, orange_left_bar, 0, 0);
			g.tileRop(Graphics.ROP_SRC_ALPHA, 17, height-11, width-27, 11, orange_bottom_bar, 0, 0);
			g.tileRop(Graphics.ROP_SRC_ALPHA, width-15, 10, 9, height-21, orange_right_bar, 0, 0);

			//draw inside bubble
			//g.tileRop(Graphics.ROP_SRC_ALPHA, 17, 9, width-27, height-19, grey_inside_bubble, 0, 0);
			g.setColor(0x00ECECEC);
			g.fillRect(17, 9, width-28, height-19);
			
		} else {
			//draw corners
			g.drawBitmap(0, 0, 17, 17, blue_bubble, 0, 0); 					//left top
			g.drawBitmap(width-11, 0, 10, 10, blue_bubble, 36, 0);				//right top
			g.drawBitmap(0, height-11, 17, 11, blue_bubble, 0, 17);			//left bottom
			g.drawBitmap(width-11, height-11, 11, 11, blue_bubble, 36, 17);	//right bottom
			//draw borders
			g.tileRop(Graphics.ROP_SRC_ALPHA, 17, 0, width-27, 9, blue_top_bar, 0, 0);
			g.tileRop(Graphics.ROP_SRC_ALPHA, 7, 17, 10, height-28, blue_left_bar, 0, 0);
			g.tileRop(Graphics.ROP_SRC_ALPHA, 17, height-11, width-27, 11, blue_bottom_bar, 0, 0);
			g.tileRop(Graphics.ROP_SRC_ALPHA, width-15, 10, 9, height-21, blue_right_bar, 0, 0);

			//draw inside bubble
			//g.tileRop(Graphics.ROP_SRC_ALPHA, 17, 10, width-27, height-19, grey_inside_bubble, 0, 0);
			g.setColor(0x00FAFAFA);
			g.fillRect(17, 9, width-28, height-19);
		}
		g.setColor(col);		
		super.paintBackground(g);
	}
	protected void sublayout(int width, int height) {
		//get the maximum width of the bubble
		int maxBubbleWidth=width*3/4;
		
		//get the text width
		/*int realBuddyNameWidth = Font.getDefault().getAdvance(messageModel.getBuddyName());
		int realMessageWidth = Font.getDefault().getAdvance(messageModel.getMessage());*/
		int realBuddyNameWidth = Font.getDefault().getAdvance(buddynameEditField.getText());
		int realMessageWidth = Font.getDefault().getAdvance(messageEditField.getText());
		
		//call layoutChild on the contents of the bubble
		layoutChild(buddynameEditField, Math.min(maxBubbleWidth, realBuddyNameWidth), height);
		layoutChild(messageEditField, Math.min(maxBubbleWidth, realMessageWidth), height);
		
		//position the elements
		setPositionChild(buddynameEditField, buddynameEditField.getMarginLeft(), buddynameEditField.getMarginTop());
		setPositionChild(messageEditField, messageEditField.getMarginLeft(), buddynameEditField.getHeight()+buddynameEditField.getMarginBottom()+buddynameEditField.getMarginTop()+messageEditField.getMarginTop());
				
		/*//maximum of text width and wrap width
		int w=Math.max(Math.min(realMessageWidth,maxBubbleWidth), Math.min(realBuddyNameWidth, maxBubbleWidth));		
		int leftrightMargin = Math.max(buddynameEditField.getMarginLeft()+buddynameEditField.getMarginRight(), messageEditField.getMarginLeft()+messageEditField.getMarginRight());
		
		//set the size of the bubble
		setExtent(w+leftrightMargin, messageEditField.getHeight()+buddynameEditField.getHeight()+
				messageEditField.getMarginTop()+messageEditField.getMarginBottom()+buddynameEditField.getMarginTop()+buddynameEditField.getMarginBottom());*/
		
		setExtent(width, messageEditField.getHeight()+buddynameEditField.getHeight()+
				messageEditField.getMarginTop()+messageEditField.getMarginBottom()+buddynameEditField.getMarginTop()+buddynameEditField.getMarginBottom());
	}
}
