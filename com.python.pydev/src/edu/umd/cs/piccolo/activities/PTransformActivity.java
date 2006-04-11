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
package edu.umd.cs.piccolo.activities;

import java.awt.geom.AffineTransform;

import edu.umd.cs.piccolo.util.PAffineTransform;

/**
 * <b>PTransformActivity</b> interpolates between two transforms setting its
 * target's transform as it goes. See PNode. animate*() for an example of this
 * activity in used. The source transform is retrieved from the target just
 * before the animation is scheduled to start.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PTransformActivity extends PInterpolatingActivity {

	private static PAffineTransform STATIC_TRANSFORM = new PAffineTransform();

	private double[] source;
	private double[] destination;
	private Target target;

	/**
	 * <b>Target</b> Objects that want to get transformed by the transform 
	 * activity must implement this interface. See PNode.animateToTransform() 
	 * for one way to do this.
	 */
	public interface Target {
		
		/**
		 * This will be called by the transform activity for each new transform
		 * that it computes while it is stepping.
		 */
		public void setTransform(AffineTransform aTransform);
		
		/**
		 * This method is called right before the transform activity starts. That
		 * way an object is always animated from its current position.
		 */
		public void getSourceMatrix(double[] aSource);
	}

	public PTransformActivity(long duration, long stepRate, Target aTarget) {
		this(duration, stepRate, aTarget, null);
	}
	
	public PTransformActivity(long duration, long stepRate, Target aTarget, AffineTransform aDestination) {
		this(duration, stepRate, 1, PInterpolatingActivity.SOURCE_TO_DESTINATION, aTarget, aDestination);
	}

	/**
	 * Create a new PTransformActivity.
	 * <P>
	 * @param duration the length of one loop of the activity
	 * @param stepRate the amount of time between steps of the activity
	 * @param loopCount number of times the activity should reschedule itself
	 * @param mode defines how the activity interpolates between states
	 * @param aTarget the object that the activity will be applied to and where
	 * the source state will be taken from.
	 * @param aDestination the destination color state
	 */
	public PTransformActivity(long duration, long stepRate, int loopCount, int mode, Target aTarget, AffineTransform aDestination) {
		super(duration, stepRate, loopCount, mode);
		source = new double[6];
		destination = new double[6];
		target = aTarget;
		if (aDestination != null) aDestination.getMatrix(destination);
	}
	
	protected boolean isAnimation() {
		return true;
	}

	/**
	 * Return the final transform that will be set on the transform activities
	 * target when the transform activity stops stepping.
	 */
	public double[] getDestinationTransform() {
		return destination;
	}

	/**
	 * Set the final transform that will be set on the transform activities
	 * target when the transform activity stops stepping.
	 */
	public void setDestinationTransform(double[] newDestination) {
		destination = newDestination;
	}
	
	protected void activityStarted() { 
		if (getFirstLoop()) target.getSourceMatrix(source);
		super.activityStarted();
	}
	
	public void setRelativeTargetValue(float zeroToOne) {
		super.setRelativeTargetValue(zeroToOne);

		STATIC_TRANSFORM.setTransform(source[0] + (zeroToOne * (destination[0] - source[0])),
									  source[1] + (zeroToOne * (destination[1] - source[1])),
									  source[2] + (zeroToOne * (destination[2] - source[2])),
									  source[3] + (zeroToOne * (destination[3] - source[3])),
									  source[4] + (zeroToOne * (destination[4] - source[4])),
									  source[5] + (zeroToOne * (destination[5] - source[5])));
									  
		target.setTransform(STATIC_TRANSFORM);
	}
	
	//****************************************************************
	// Debugging - methods for debugging
	//****************************************************************

	/**
	 * Returns a string representing the state of this activity. This method is
	 * intended to be used only for debugging purposes, and the content and
	 * format of the returned string may vary between implementations. The
	 * returned string may be empty but may not be <code>null</code>.
	 *
	 * @return  a string representation of this activity's state
	 */
	protected String paramString() {
		StringBuffer result = new StringBuffer();

		result.append("source=" + (source == null ? "null" : source.toString()));
		result.append(",destination=" + (destination == null ? "null" : destination.toString()));
		result.append(',');
		result.append(super.paramString());
		
		return result.toString();
	}	
}
