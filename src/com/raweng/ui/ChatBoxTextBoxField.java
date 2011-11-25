package com.raweng.ui;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.container.VerticalFieldManager;

public class ChatBoxTextBoxField extends VerticalFieldManager {

	//define some variables to be used
	//in the class
	private int managerWidth;
	private int managerHeight;
	private EditField editField;
	
	
	private static final Bitmap blue_bubble = Bitmap.getBitmapResource("blue_bubble.png");
	private static final Bitmap blue_left_bar = Bitmap.getBitmapResource("blue_left_bar.png");
	private static final Bitmap blue_top_bar = Bitmap.getBitmapResource("blue_top_bar.png");
	private static final Bitmap blue_right_bar = Bitmap.getBitmapResource("blue_right_bar.png");
	private static final Bitmap blue_bottom_bar = Bitmap.getBitmapResource("blue_bottom_bar.png");


	public ChatBoxTextBoxField(int width, int height, long style) {
		super(Manager.NO_VERTICAL_SCROLL | style);
		managerWidth = width;
		managerHeight = height;

		VerticalFieldManager vfm = new VerticalFieldManager(Manager.VERTICAL_SCROLL);

		editField = new EditField(){
			public void paint(Graphics g) {
				getManager().invalidate();
				super.paint(g);
			}
		};
		editField.setNonSpellCheckable(true);
		

		vfm.setPadding(10, 20, 10, 20);
		vfm.add(editField);
		add(vfm);
	}

	/*public void paint(Graphics g) {
		int color = g.getColor();
		
		g.setColor(Color.BLUE);
		g.drawRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.WHITE);
		g.fillRect(1, 1, getWidth()-2, getHeight()-2);
		
		g.setColor(color);
		super.paint(g);
	}*/
	
	protected void paintBackground(Graphics g) {
		//paint my bubble
		int col=g.getColor();

		int height = this.getContentHeight();		
		//int width = this.getContentWidth();
		int width = this.getWidth();
		
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
			
		g.setColor(col);		
		super.paintBackground(g);
	}
	

	public void sublayout(int width, int height) {
		if (managerWidth == 0) {
			managerWidth = width;
		}
		if (managerHeight == 0) {
			managerHeight = height;
		}
		super.sublayout(managerWidth, managerHeight);
		setExtent(managerWidth,managerHeight);
	}

	public String getText() {
		return editField.getText();
	}
	public void setText(String text) {
		editField.setText(text);
	}
}