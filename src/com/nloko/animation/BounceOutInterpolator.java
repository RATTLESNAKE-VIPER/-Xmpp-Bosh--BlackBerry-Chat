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

/**
 * Bounce on the target like a ball
 * Formula from Robert Penner. Licensed under BSD http://robertpenner.com/easing_terms_of_use.html
 * @author Neil
 *
 */
public class BounceOutInterpolator implements Interpolator {

	public int getInterpolatedTime(int time, int duration) {
		float t = (float)time / (float)duration;
		float i;
		
		if (t < (1f / 2.75f)) {
			i = (7.5625f * t * t);
		} else if (t < (2f / 2.75f)) {
			t -= 1.5f / 2.75f;
			i = (7.5625f * t * t + .75f);
		} else if (t < (2.5f / 2.75f)) {
			t-= 2.25f / 2.75f;
			i = (7.5625f * t * t + .9375f);
		} else {
			t -= 2.625f / 2.75f;
			i = (7.5625f * t * t + .984375f);
		}
		
		return (int)(i * duration);
	}
}
