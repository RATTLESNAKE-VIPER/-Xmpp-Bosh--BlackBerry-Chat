package com.raweng.rawchat;

import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.NullField;
import net.rim.device.api.ui.component.ObjectChoiceField;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;

import com.raweng.ui.TableLayoutManager;
import com.raweng.ui.TextBoxField;
import com.raweng.xmppservice.Connection;

public class LoginScreen extends MainScreen {

	public static BChat btalk;
	private Connection connection;
	private boolean saved;
	
	private Font labelFont = Font.getDefault().derive(Font.BOLD, 5, Ui.UNITS_pt);
	private Font textFont = Font.getDefault().derive(Font.PLAIN, 5, Ui.UNITS_pt);
	private TextBoxField usernameTextField;
	private TextBoxField domainTextField;
	private TextBoxField passwordTextField;
	private TextBoxField connectServerTextField;
	private TextBoxField connectPortTextField;
	private TextBoxField boshUrlTextField;
	private ButtonField saveButtonField;
	private ObjectChoiceField networkTypeChoiceField;
	
	
	public final static int NETWORK_TYPE_INDEX_ARRAY[] = {0, 1, 2, 4, 8, 16};
	public final static String NETWORK_TYPE_NAME_ARRAY[] = {"Auto", "WIFI", "BES", "BIS", "Direct TCP","WAP"};
	
	

	public LoginScreen(Connection connection, boolean saved) {
		super(NO_VERTICAL_SCROLL | NO_VERTICAL_SCROLLBAR);
		setTitle("Login");
		
		this.connection = connection;
		this.saved = saved;
		this.initUI();
	}

	private void initUI() {
		VerticalFieldManager rootManager = new VerticalFieldManager(USE_ALL_WIDTH | USE_ALL_HEIGHT | Manager.VERTICAL_SCROLL | Manager.NO_VERTICAL_SCROLLBAR);
		
		TableLayoutManager fieldsContainer = new TableLayoutManager(new int[] {
				TableLayoutManager.USE_PREFERRED_SIZE,
				TableLayoutManager.FIXED_WIDTH }, new int[] { 0, 200 }, 2, 5,
				Manager.NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL | NO_VERTICAL_SCROLLBAR | FIELD_HCENTER);
		fieldsContainer.setMargin(10, 10, 10, 10);
		
		
		
		LabelField usernameLabelField = new LabelField("Username: ", Field.FIELD_VCENTER);
		usernameLabelField.setFont(labelFont);
		usernameTextField = new TextBoxField();
		usernameTextField.setFont(textFont);
		
		
		LabelField domainLabelField = new LabelField("Domain: ", Field.FIELD_VCENTER);
		domainLabelField.setFont(labelFont);
		domainTextField = new TextBoxField();
		domainTextField.setFont(textFont);		
		
		
		LabelField passwordLabelField = new LabelField("Password: ", Field.FIELD_VCENTER);
		passwordLabelField.setFont(labelFont);
		passwordTextField = new TextBoxField(true);
		passwordTextField.setFont(textFont);
		
		
		LabelField connectServerLabelField = new LabelField("Connect Server: ", Field.FIELD_VCENTER);
		connectServerLabelField.setFont(labelFont);
		connectServerTextField = new TextBoxField();
		connectServerTextField.setFont(textFont);
		
		
		LabelField connectPortLabelField = new LabelField("Connect Port: ", Field.FIELD_VCENTER);
		connectPortLabelField.setFont(labelFont);
		connectPortTextField = new TextBoxField();
		connectPortTextField.setFont(textFont);
		
		
		LabelField boshUrlLabelField = new LabelField("Bosh url: ", Field.FIELD_VCENTER);
		boshUrlLabelField.setFont(labelFont);
		boshUrlTextField = new TextBoxField();
		boshUrlTextField.setFont(textFont);
		
		
		LabelField networkTypeLabelField = new LabelField("Network Type: ", Field.FIELD_VCENTER);
		networkTypeLabelField.setFont(labelFont);
		networkTypeChoiceField = new ObjectChoiceField("", NETWORK_TYPE_NAME_ARRAY, 0, Field.FIELD_LEFT | Field.FIELD_VCENTER);
		networkTypeChoiceField.setFont(textFont);
		
		
		saveButtonField = new ButtonField("Save") {
			protected boolean keyChar(char key, int status, int time) {
				if (key == Keypad.KEY_ENTER) {
					return login();
				} else {
					return false;
				}
			}
			protected boolean navigationClick(int status, int time) {
				return login();
			}
		};
		saveButtonField.setFont(textFont);
		
		
		fieldsContainer.add(usernameLabelField);
		fieldsContainer.add(usernameTextField);
		fieldsContainer.add(domainLabelField);
		fieldsContainer.add(domainTextField);
		fieldsContainer.add(passwordLabelField);
		fieldsContainer.add(passwordTextField);
		fieldsContainer.add(connectServerLabelField);
		fieldsContainer.add(connectServerTextField);
		fieldsContainer.add(connectPortLabelField);
		fieldsContainer.add(connectPortTextField);
		fieldsContainer.add(boshUrlLabelField);
		fieldsContainer.add(boshUrlTextField);
		fieldsContainer.add(networkTypeLabelField);
		fieldsContainer.add(networkTypeChoiceField);
		fieldsContainer.add(new NullField(Field.NON_FOCUSABLE));
		fieldsContainer.add(saveButtonField);
		
		rootManager.add(fieldsContainer);
		add(rootManager);
		
		
		
		if (this.saved) {
			usernameTextField.setText(this.connection.getUsername());
			domainTextField.setText(this.connection.getHost());
			passwordTextField.setText(this.connection.getPassword());
			connectServerTextField.setText(this.connection.getServer());
			connectPortTextField.setText(this.connection.getPort());
			boshUrlTextField.setText(this.connection.getHttpburl());
			networkTypeChoiceField.setSelectedIndex(getNetworkTypeNameArrayIndex(this.connection.getNetworkType()));
		}
		
		
		
		
		
		this.addMenuItem(new MenuItem("Login", 0, 0) {
			public void run() {
				login();
			}
		});
		
		/*this.addMenuItem(new MenuItem("Options", 0, 0){
			public void run() {
				btalk.pushScreen(new SettingsScreen());
			}
		});*/
		
		
		if (BChat.DEBUG) {
			this.addMenuItem(new MenuItem("Debug console", 0x00030006, 0) {
				public void run() {
					btalk.pushScreen(BChat.debugConsole);
				}
			});
		}
	}
	
	private boolean login() {
		if (usernameTextField.getText().length() == 0 || passwordTextField.getText().length() == 0) {
			Dialog.alert("Invalid username/password!");
			return true;
		}
		
		ServerModel serverDef = new ServerModel();
		serverDef.useWifi = false;

		if (connectServerTextField.getText().length() <= 0 && boshUrlTextField.getText().length() <= 0) {
			Dialog.alert("Invalid server address");
			return true;
		}

		serverDef.server = connectServerTextField.getText();
		serverDef.boshUrl = boshUrlTextField.getText();
		if (boshUrlTextField.getText().length() > 0 && connectServerTextField.getText().length() == 0) {
			serverDef.useBosh = true;
		} else {
			serverDef.useBosh = false;
		}
		
		serverDef.port = connectPortTextField.getText().trim();
		if (serverDef.port.equals("5223")) {
			serverDef.usessl = true;
		} else {
			serverDef.usessl = false;
		}

		connection.getChatHandlerInstance().login(usernameTextField.getText(), passwordTextField.getText(), domainTextField.getText(), serverDef, NETWORK_TYPE_INDEX_ARRAY[networkTypeChoiceField.getSelectedIndex()]);
		return true;
	}
	
	public boolean onClose() {
		this.close();
		return true;
	}
	
	protected boolean onSavePrompt() {
		return true;
	}
	
	
	
	public static int getNetworkTypeNameArrayIndex(int networkTypeIndexArrayIndex){
		for(int i = 0; i < NETWORK_TYPE_INDEX_ARRAY.length; i++){
			if(NETWORK_TYPE_INDEX_ARRAY[i] == networkTypeIndexArrayIndex){
				return i;
			}
		}
		return 0;
	}

	public static int getNetworkTypeIndexArrayIndex(String networkName){
		for(int i = 0; i < NETWORK_TYPE_NAME_ARRAY.length; i++){
			if(NETWORK_TYPE_NAME_ARRAY[i].equals(networkName)){
				return i;
			}
		}
		return 0;
	}

}
