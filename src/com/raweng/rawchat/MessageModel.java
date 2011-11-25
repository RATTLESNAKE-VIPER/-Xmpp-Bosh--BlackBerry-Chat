package com.raweng.rawchat;

import java.util.Vector;

public class MessageModel {
	public static final int TYPE_IN = 0;
	public static final int TYPE_OUT = 1;
	private int type;
	private String buddyname;
	private String message;
	private Vector assets;
	

	public MessageModel(int t, String buddyname, String msg) {
		this.type = t;
		this.buddyname = buddyname;
		this.message = msg;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	public String getBuddyName() {
		return buddyname;
	}

	public void setBuddyName(String buddyname) {
		this.buddyname = buddyname;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Vector getAssets() {
		return assets;
	}

	public void setAssets(Vector assets) {
		this.assets = assets;
	}
}