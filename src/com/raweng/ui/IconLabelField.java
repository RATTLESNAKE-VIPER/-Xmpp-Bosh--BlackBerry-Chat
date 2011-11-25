package com.raweng.ui;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.DrawStyle;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.component.LabelField;

public class IconLabelField extends LabelField {
	private Bitmap _icon;
	private String _text;
	
	public IconLabelField(Bitmap icon, String text) {
		super("                                  ");
		_icon = icon;
		_text = text;
	}
	
	public void paint(Graphics g) {
		if (_text != null && _icon != null ) {
			g.drawBitmap(this.getLeft()+5, this.getTop()+((this.getHeight()-14)>>1), 14, 14, _icon, 0, 0);
			g.drawText(_text, 25, this.getTop(), DrawStyle.ELLIPSIS, this.getWidth()-25);
		}		
	}
	
	public void setIcon(Bitmap icon) {
		_icon = icon;
	}
	
	public void setText(String text) {
		_text = text;
	}
}
