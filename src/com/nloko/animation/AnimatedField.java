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

import net.rim.device.api.ui.Field;

/**
 * Encapsulate animation state information within each animated field
 * @author Neil
 *
 */
public interface AnimatedField {
	public static interface AnimatedFieldController {
		/**
		 * Update the current field position based on the progress of an animation
		 * @param time
		 * @param duration
		 */
		void updatePosition(int time, int duration);
	}
	/**
	 * The actual field being animated
	 * @return
	 */
	Field getField();

	/**
	 * Get the current position of the field
	 * @return
	 */
	Point getPosition();
	/**
	 * Set fields start point for animation
	 * @return
	 */
	Point getStartPosition();
	/**
	 * Set fields start point
	 * @return
	 */
	void setStartPosition(Point p);
	/**
	 * Get fields target point for animation
	 * @return
	 */
	Point getTargetPosition();
	/**
	 * Set fields target point
	 * @return
	 */
	void setTargetPosition(Point p);
}
