package com.raweng.rawchat;

import java.util.Vector;

import net.rim.device.api.system.Application;
import net.rim.device.api.system.Display;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Manager;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.NullField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.VerticalFieldManager;

import com.nloko.animation.AnimatedField;
import com.nloko.animation.Animation;
import com.nloko.animation.BounceOutInterpolator;
import com.nloko.animation.Point;
import com.nloko.animation.AnimatedField.AnimatedFieldController;
import com.nloko.animation.Animation.AnimationListener;

public class NewMessageNotificationManager extends Manager implements AnimationListener {
	private int _closeEvent = -1; 

	private static final int IDLE = 0;
	private static final int ANIMATING_DOWN = 1;
	private static final int ANIMATING_UP = 2;

	private volatile int _state = IDLE;

	private final Animation _animation;		
	private final int _duration = 1000;

	private volatile boolean _isAnimating;

	private AnimatedField _label;
	private AnimatedFieldController _controller;	


	private Vector newNotificationsList = new Vector();
	private LabelField nameLabelField;
	private LabelField messageLabelField;
	private Manager newNotificationManager;



	public NewMessageNotificationManager() {
		super(USE_ALL_WIDTH);

		_animation = new Animation(_duration);
		_animation.setListener(this);
		//_animation.setInterpolator(new OvershootInterpolator(4f));
		_animation.setInterpolator(new BounceOutInterpolator());



		newNotificationManager = createNewMessageNotificationView();
		_label = this.getAnimatedField(newNotificationManager);
		_controller = new BounceController(_label);	
		add(_label.getField());
	}




	private AnimatedField getAnimatedField(final Field _field) {
		AnimatedField _label = new AnimatedField() {
			private final int WIDTH = Display.getWidth();
			//private final int OFFSET_FROM_CENTER = WIDTH - (WIDTH + Font.getDefault().getAdvance(TEXT)) / 2;
			private final int OFFSET_FROM_CENTER = 0;//WIDTH - (WIDTH + _textWidth) / 2;

			private final Point _start = new Point(OFFSET_FROM_CENTER, -_field.getPreferredHeight());
			private final Point _pos = new Point(OFFSET_FROM_CENTER, _start.y);
			private final Point _dest = new Point(_pos.x, 0);

			public Field getField() {
				return _field;
			}

			public Point getPosition() {
				return _pos;
			}

			public Point getStartPosition() {
				return _start;
			}

			public Point getTargetPosition() {
				return _dest;
			}

			public void setStartPosition(Point p) {
				_start.x = p.x;
				_start.y = p.y;
			}

			public void setTargetPosition(Point p) {
				_dest.x = p.x;
				_dest.y = p.y;
			}
		};
		return _label;
	}


	private static class BounceController implements AnimatedFieldController {
		private final AnimatedField _field;
		public BounceController(AnimatedField field) {
			_field = field;
		}

		public void updatePosition(int time, int duration) {
			_field.getPosition().y = _field.getStartPosition().y + 
			(_field.getTargetPosition().y - _field.getStartPosition().y)  * time / duration;
		}
	}






	public void stopAnimation() {
		_isAnimating = false;
		_animation.stop();
		newNotificationsList.removeAllElements();
	}





	public void startAnimation() {
		_isAnimating = true;

		if (_label.getStartPosition().y < _label.getTargetPosition().y) _state = ANIMATING_DOWN;
		else _state = ANIMATING_UP;

		_animation.start();
	}

	public boolean isAnimating() {
		return _isAnimating;
	}

	public boolean isCloseScheduled() {
		return _closeEvent >= 0;
	}

	public void cancelClose() {
		UiApplication.getApplication().cancelInvokeLater(_closeEvent);
		_closeEvent = -1;
	}

	protected void sublayout(int maxwidth, int maxheight) {
		if (_label != null) {
			updateAnimation(_label);
		}		
		setExtent(maxwidth, Math.max(0, getPreferredHeight()));		
	}

	/**
	 * Helper method to update the position of fields
	 * @param af
	 */
	private void updateAnimation(AnimatedField af) {
		Field f = af.getField();
		final int width = f.getPreferredWidth();
		final int height = f.getPreferredHeight();

		if (!_isAnimating) layoutChild(f, width, height);	

		setPositionChild(f, 
				af.getPosition().x, 
				af.getPosition().y);
	}

	public int getPreferredHeight() {
		if (_label != null) {
			return Math.max(0, _label.getField().getPreferredHeight() + _label.getPosition().y);
		}
		return 0;
	}

	public int getPreferredWidth() {
		return Display.getWidth();
	}

	protected void paint(Graphics graphics) {
		int color = graphics.getColor();
		graphics.setColor(Color.BLACK);
		graphics.setGlobalAlpha(150);
		graphics.fillRect(0, 0, getPreferredWidth(), getPreferredHeight());

		graphics.setColor(color);
		graphics.setGlobalAlpha(255);
		super.paint(graphics);
	}

	void swapAnimationTargets() {
		if (_label != null) {
			Point target = new Point(_label.getTargetPosition().x,
					_label.getTargetPosition().y);

			_label.setTargetPosition(_label.getStartPosition());
			_label.setStartPosition(target);
		}		
	}

	public void onNewFrame(int time) {
		_controller.updatePosition(time, _duration);

		synchronized(Application.getEventLock()) {
			// tell the framework to update the field layout
			updateLayout();

			if (!(_isAnimating = !(time == _duration))) {
				swapAnimationTargets();

				if (_state == ANIMATING_DOWN) {					
					_closeEvent = UiApplication.getApplication().invokeLater(new Runnable() {
						public void run() {
							startAnimation();
						}
					} , 1000, false);
				} else {
					_state = IDLE;
					_closeEvent = -1;


					// Retrieve notifications from the queue
					if (newNotificationsList.size() > 0) {
						if (isCloseScheduled()) {
							cancelClose();
							swapAnimationTargets();
						}

						UiApplication.getApplication().invokeLater(new Runnable() {
							public void run() {
								newNotificationsList.removeElementAt(0);
								if (newNotificationsList.size() > 0) {
									NotificationMessage msg = (NotificationMessage) newNotificationsList.elementAt(0);
									nameLabelField.setText(msg.name + ": ");
									messageLabelField.setText(msg.message);
									startAnimation();
								}
							}
						});	
					}
				}
			}
		}
	}




	public void newMessage(final String from, final String message) {	
		NotificationMessage notificationMessage = new NotificationMessage(from, message);

		if (newNotificationsList.size() == 0) {
			newNotificationsList.addElement(notificationMessage);
			NotificationMessage msg = (NotificationMessage) newNotificationsList.elementAt(0);			
			nameLabelField.setText(msg.name + ": ");
			messageLabelField.setText(msg.message);
			this.startAnimation();

		} else {
			newNotificationsList.addElement(notificationMessage);
		}
	}


	private Manager createNewMessageNotificationView() {
		VerticalFieldManager newMessageContainer = new VerticalFieldManager(Field.USE_ALL_WIDTH);		
		HorizontalFieldManager hfm = new HorizontalFieldManager(Field.USE_ALL_WIDTH);

		nameLabelField = new LabelField() {
			protected void paint(Graphics g) {
				g.setColor(Color.WHITE);					
				super.paint(g);
			}
		};
		nameLabelField.setFont(Font.getDefault().derive(Font.PLAIN, 16));
		nameLabelField.setMargin(5, 5, 5, 5);

		messageLabelField = new LabelField() {
			protected void paint(Graphics g) {
				g.setColor(Color.WHITE);					
				super.paint(g);
			}
		};
		messageLabelField.setFont(Font.getDefault().derive(Font.PLAIN, 16));
		messageLabelField.setMargin(5, 5, 5, 5);

		
		hfm.add(nameLabelField);
		hfm.add(messageLabelField);
		hfm.add(new NullField(NON_FOCUSABLE));
		newMessageContainer.add(hfm);

		return newMessageContainer;
	}





	private class NotificationMessage {		
		private String name;
		private String message;

		private NotificationMessage(String name, String message) {
			this.name = name;
			this.message = message;
		}
	}
}