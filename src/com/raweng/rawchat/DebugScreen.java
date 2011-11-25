package com.raweng.rawchat;

import net.rim.device.api.i18n.DateFormat;
import net.rim.device.api.ui.component.RichTextField;
import net.rim.device.api.ui.container.MainScreen;

public class DebugScreen extends MainScreen {
	
	private static DateFormat dateGen = DateFormat.getInstance(DateFormat.TIME_DEFAULT);
	
	public void addDebugMsg(final String msg) {
		long time = System.currentTimeMillis();
		String timeStr = dateGen.formatLocal(time);
		
		this.add(new RichTextField(timeStr+" : "+msg));
	}

}
