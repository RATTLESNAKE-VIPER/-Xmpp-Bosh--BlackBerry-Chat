package com.raweng.ui;

import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.EditField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.VerticalFieldManager;


public class TextBoxField extends VerticalFieldManager {

	/* Variables */
	public final static int TextField_HEIGHT = 25;
	public final static int TextField_WIDTH = 200;	
	public final static int BACKGROUND_COLOR = 0x00ffffff;
	private int managerWidth;
	private int managerHeight;

	/* UI Variables */
	private BasicEditField editField;
	private static long style = EditField.NO_NEWLINE;

	public TextBoxField(){
		this(TextField_WIDTH, TextField_HEIGHT, false, style);
	}

	public TextBoxField(long style){		
		this(TextField_WIDTH, TextField_HEIGHT, false, style);
	}

	public TextBoxField(boolean isPasswordEditField){
		this(TextField_WIDTH, TextField_HEIGHT, isPasswordEditField, style);
	}

	public TextBoxField(int width, int height, boolean isPasswordEditField, long style) {
		super(Field.FIELD_VCENTER | Manager.NO_VERTICAL_SCROLL);
		managerWidth = width;
		managerHeight = height;

		HorizontalFieldManager hfm = new HorizontalFieldManager(Manager.HORIZONTAL_SCROLL);	
		if(isPasswordEditField){
			editField = new PasswordEditField("", "", 50, PasswordEditField.NO_NEWLINE) {
				public void paint(Graphics g) {
					getManager().invalidate();
					super.paint(g);
				}
			};	
		}
		else{
			editField = new EditField(style) {
				public void paint(Graphics g) {
					getManager().invalidate();
					super.paint(g);
				}
			};	
		}
		editField.setNonSpellCheckable(true);
		if(editField.getPreferredHeight() >= managerHeight){
			managerHeight = editField.getPreferredHeight() + 2;
		}

		hfm.add(editField);			
		add(hfm);
	}

	public void paint(Graphics g) {
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, getWidth(), managerHeight);
		g.setColor(BACKGROUND_COLOR);
		g.fillRect(1, 1, (getWidth()-2), managerHeight - 2); 
		g.setColor(Color.BLACK);
		super.paint(g);
	}

	public void sublayout(int width, int height) {
		super.sublayout(managerWidth, managerHeight);
		if (managerWidth == 0) {
			managerWidth = width;
		}
		if (managerHeight == 0) {
			managerHeight = editField.getPreferredHeight();
		}
		managerWidth = Math.min(managerWidth, width);		
		if (getFieldCount() == 1) {  
			Field field = getField(0);               
			layoutChild(field, managerWidth, managerHeight);   
			setPositionChild(field, 2, (managerHeight - field.getPreferredHeight()) / 2);      
			setExtent(managerWidth, managerHeight);   
		}  
	}

	public void setEditable(boolean edit){
		editField.setEditable(edit);
	}

	public String getText() {
		return editField.getText();
	}

	public void setText(String text) {
		editField.setText(text);
	}

}