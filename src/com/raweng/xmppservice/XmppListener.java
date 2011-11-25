/*
 * Copyright 2006 Swen Kummer, Dustin Hass, Sven Jost
 * modified by Yuan-Chu Tai
 * http://jxa.sourceforge.net/
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. Mobber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with mobber; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA .
 */

package com.raweng.xmppservice;

/**
 * Interface class to implement events
 * 
 * @author Swen Kummer, Dustin Hass, Sven Jost
 * @version 2.0
 * @since 1.0
 */
public interface XmppListener {

	//public void onDebug(final String msg);
	/**
	 * This event is sent when a parser or connection error occurs.
	 */
	public void onConnFailed(final String msg);

	/**
	 * This event occurs when the login/authentication process succeeds.
	 */
	public void onAuth(final String resource);

	/**
	 * This event occurs when the login/authentication process fails.
	 * 
	 * @param message some error information
	 */
	public void onAuthFailed(final String message);

	/**
	 * This event is sent when a message arrives.
	 * 
	 * @param from the jid of the sender
	 * @param body the message text
	 * @param id 
	 * @param id2 
	 */
	public void onMessageEvent(final String from, final String to, final String body, final String id);

	/**
	 * This event occurs when someone has removed you from his roster (o rly?)
	 * 
	 * @param jid the jid of the remover
	 */
	//public void onContactRemoveEvent(final String jid);

	/**
	 * This event occurs for each contact in roster when the roster is queried.
	 * 
	 * @param jid the jid of the contact
	 * @param name the nickname of the contact
	 * @param group the group in which the contact is saved
	 * @param subscription the subscription status of the contact
	 */
	public void onContactEvent(final String jid, final String name, final String group, final String subscription);

	public void onContactOverEvent();
	/**
	 * <p>
	 * This event occurs when a presence message comes from jabber server. This
	 * can also be your own jid. The presence can be one of the following:
	 * </p>
	 * 
	 * <ul>
	 * <li><code>blank</code>: user is online</li>
	 * <li>chat: user is free to chat</li>
	 * <li>away: user is away</li>
	 * <li>xa: user is not available (extended away).</li>
	 * <li>dnd: user is busy (do not disturb).</li>
	 * </ul>
	 * 
	 * <p>
	 * An offline user will send no status message at all.
	 * </p>
	 * 
	 * @param jid the JID of the contact that changed his status
	 * @param status the display status
	 */
	public void onStatusEvent(final String jid, final String show, final String status);

	/**
	 * This event is sent when a subscription request arrives. This means
	 * someone has allowed you to see his status.
	 * 
	 * @param jid the jid of the one who wants to subscribe to you
	 */
	public void onSubscribeEvent(final String jid);

	/**
	 * This event is sent when a subscription remove event arrives. This means
	 * someone has taken away your right to see his status.
	 * 
	 * @param jid the jid of the one who removes your subscription
	 */
	public void onUnsubscribeEvent(final String jid);
	
	
	
	public void onRosterVCardEvent(final String jid, final String base64StringValue);

};
