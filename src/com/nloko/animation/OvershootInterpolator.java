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

public class OvershootInterpolator implements Interpolator {
	private float _tension = 2.0f;
	
	public OvershootInterpolator() {
	}
	
	public OvershootInterpolator(float tension) {
		_tension = tension;
	}
	
	public int getInterpolatedTime(int time, int duration) {
		float t = (float)time / (float)duration; 
		t -= 1.0f;
        
		float i = t * t * ((_tension + 1) * t + _tension) + 1.0f;
        
        return (int)(i * duration);
	}
}
