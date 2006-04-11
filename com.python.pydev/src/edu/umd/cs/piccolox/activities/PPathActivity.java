/*
 * Copyright (c) 2002-@year@, University of Maryland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the University of Maryland nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Piccolo was written at the Human-Computer Interaction Laboratory www.cs.umd.edu/hcil by Jesse Grosjean
 * under the supervision of Ben Bederson. The Piccolo website is www.cs.umd.edu/hcil/piccolo.
 */
package edu.umd.cs.piccolox.activities;

import edu.umd.cs.piccolo.activities.PInterpolatingActivity;

/**
 * <b>PPathActivity</b> is the abstract base class for all path
 * activity interpolators. Path activities interpolate between multiple states
 * over the duration of the activity.
 * <p>
 * Knots are used to determine when in time the activity should move from state
 * to state. Knot values should be increasing in value from 0 to 1 inclusive.
 * This class is based on the Java 3D PathInterpolator object, see that class
 * documentation for more information on the basic concepts used in this classes
 * design.
 * <p>
 * See PPositionPathActivity for a concrete path activity that will animate
 * through a list of points.
 * <p>
 * @version  1.0
 * @author Jesse Grosjean
 */
public abstract class PPathActivity extends PInterpolatingActivity {
	
	protected float[] knots;

	public PPathActivity(long duration, long stepRate, float[] knots) {
		this(duration, stepRate, 0, PInterpolatingActivity.SOURCE_TO_DESTINATION, knots);
	}
			
	public PPathActivity(long duration, long stepRate, int loopCount, int mode, float[] knots) {
		super(duration, stepRate, loopCount, mode);
		setKnots(knots);
	}
	
	public int getKnotsLength() {
		return knots.length;
	}
	
	public void setKnots(float[] knots) {
		this.knots = knots;
	}
	
	public float[] getKnots() {
		return knots;
	}

	public void setKnot(int index, float knot) {
		knots[index] = knot;
	}	
	
	public float getKnot(int index) {
		return knots[index];
	}
		
	public void setRelativeTargetValue(float zeroToOne) {
		int currentKnotIndex = 0;
		
		while (zeroToOne > knots[currentKnotIndex]) {
			currentKnotIndex++;
		}

		int startKnot = currentKnotIndex - 1;
		int endKnot = currentKnotIndex;
		
		if (startKnot < 0) startKnot = 0;
		if (endKnot > getKnotsLength() - 1) endKnot = getKnotsLength() - 1;
		
		float currentRange = knots[endKnot] - knots[startKnot];
		float currentPointOnRange = zeroToOne - knots[startKnot];
		float normalizedPointOnRange = currentPointOnRange;
		
		if (currentRange != 0) {
			normalizedPointOnRange = currentPointOnRange / currentRange;
		}
		
		setRelativeTargetValue(normalizedPointOnRange, startKnot, endKnot);		
	}
	
	public abstract void setRelativeTargetValue(float zeroToOne, int startKnot, int endKnot);
}
