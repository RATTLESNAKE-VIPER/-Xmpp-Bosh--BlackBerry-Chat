/**
 * Copyright 2010 Neil Loknath
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

package com.nloko.animation;

public class Animation {
	public static interface AnimationListener {
		void onNewFrame(int time);
	}
	
	private static final int DEFAULT_FPS = 40;
	
	private int _duration;
	private int _time;
	private int _fps = DEFAULT_FPS;
	private Thread _thread;
	private volatile boolean _running;
	
	private AnimationListener _listener;
	
	private Interpolator _interpolator = new LinearInterpolator();
	
	public Animation(int duration) {
		_duration = duration;
	}
	
	public void setInterpolator(Interpolator i) {
		if (i == null) i = new LinearInterpolator();
		_interpolator = i;
	}
	
	/**
	 * The number of frames in an animation per second
	 * @param fps
	 */
	public void setFPS(int fps) {
		_fps = fps;
	}
	
	public int getFPS() {
		return _fps;
	}
	
	/**
	 * The total duration of the animation
	 * @param duration
	 */
	public void setDuration(int duration) {
		_duration = duration;
	}
	
	public int getDuration() {
		return _duration;
	}
	
	/**
	 * The time elasped since the animation started
	 * @return
	 */
	public int getTimeElapsed() {
		return _time;
	}
	
	public void setListener(AnimationListener l) {
		_listener = l;
	}
	
	/**
	 * Start the animation
	 */
	public synchronized void start() {
		if (_thread == null) {
			_running = true;
			_thread = new Thread(new Runnable() {
				public void run() {
					
					final int idleTime = 1000 / _fps;
					final int duration = _duration;
					
					final Interpolator interpolator = _interpolator;
					
					_time = 0;
					
					try {
						while(_running && _time <= duration) {
							animate(interpolator.getInterpolatedTime(_time, duration));
							_time += idleTime;

							Thread.sleep(idleTime);
						}
						
						if (_time - idleTime < duration) animate(duration);
						
					} catch (InterruptedException e) {}
					
					stop();
				}
			});
			
			_thread.start();
		}
	}
	
	/**
	 * Stop the animation
	 */
	public synchronized void stop() {
		
		_running = false;
		
		if (_thread != null && _thread.isAlive()) {
			_thread.interrupt();
			_thread = null;
		}
	}
	
	/**
	 * 
	 * @param time
	 */
	private void animate(int time) {
		AnimationListener listener = _listener;
		if (listener != null) {
			listener.onNewFrame(time);
		}
		
	}
}
