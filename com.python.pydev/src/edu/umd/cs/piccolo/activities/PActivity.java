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

import edu.umd.cs.piccolo.util.PUtil;

/**
 * <b>PActivity</b> controls some time dependent aspect of Piccolo, such
 * as animation. Once created activities must be scheduled with the 
 * PActivityScheduler managed by the PRoot to run. They are automatically
 * removed from the scheduler when the animation has finished.
 * <P>
 * See the PNode.animate*() methods for an example of how to set up and run
 * different activities.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PActivity {
	
    public static final int TERMINATE_WITHOUT_FINISHING = 0;
    public static final int TERMINATE_AND_FINISH = 1;
    public static final int TERMINATE_AND_FINISH_IF_STEPPING = 2;

	private PActivityScheduler scheduler;
	
	private long startTime;
	private long duration;
	private long stepRate;
	private PActivityDelegate delegate;
	
	private boolean stepping;
	private long nextStepTime;
	
	/**
	 * <b>PActivityDelegate</b> is used by classes to learn about and act on the 
	 * different states that a PActivity goes through, such as when the activity
	 * starts and stops stepping.
	 */
	public interface PActivityDelegate {
		public void activityStarted(PActivity activity);
		public void activityStepped(PActivity activity);
		public void activityFinished(PActivity activity);
	}
	
	/**
	 * Constructs a new PActivity.
	 * 
	 * @param aDuration the amount of time that this activity should take to complete, -1 for infinite.
	 */
	public PActivity(long aDuration) {
		this(aDuration, PUtil.DEFAULT_ACTIVITY_STEP_RATE);
	}
	
	/**
	 * Constructs a new PActivity.
	 * 
	 * @param aDuration the amount of time that this activity should take to complete, -1 for infinite.
	 * @param aStepRate the maximum rate that this activity should receive step events.
	 */
	public PActivity(long aDuration, long aStepRate) {
		this(aDuration, aStepRate, System.currentTimeMillis());
	}
	
	/**
	 * Constructs a new PActivity.
	 * 
	 * @param aDuration the amount of time that this activity should take to complete, -1 for infinite.
	 * @param aStepRate the maximum rate that this activity should receive step events.
	 * @param aStartTime the time (relative to System.currentTimeMillis()) that
	 * this activity should start.
	 */
	public PActivity(long aDuration, long aStepRate, long aStartTime) {
		duration = aDuration;
		stepRate = aStepRate;
		startTime = aStartTime;
		nextStepTime = aStartTime;
		stepping = false;
	}
	
	//****************************************************************
	// Basics
	//****************************************************************

	/**
	 * Return the time that this activity should start running in PRoot
	 * global time. When this time is reached (or soon after) this activity
	 * will have its startStepping() method called.
	 */
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * Set the time that this activity should start running in PRoot
	 * global time. When this time is reached (or soon after) this activity
	 * will have its startStepping() method called.
	 */
	public void setStartTime(long aTriggerTime) {
		startTime = aTriggerTime;
	}
	
	/**
	 * Return the amount of time that this activity should delay
	 * between steps.
	 */
	public long getStepRate() {
		return stepRate;
	}
	
	/**
	 * Set the amount of time that this activity should delay
	 * between steps.
	 */
	public void setStepRate(long aStepRate) {
		stepRate = aStepRate;
	}

	public long getNextStepTime() {
		return nextStepTime;
	}

	/**
	 * Return the amount of time that this activity should take to complete,
	 * after the startStepping method is called.
	 */
	public long getDuration() {
		return duration;
	}
	
	/**
	 * Set the amount of time that this activity should take to complete,
	 * after the startStepping method is called.
	 */
	public void setDuration(long aDuration) {
		duration = aDuration;
	}
	
	public PActivityScheduler getActivityScheduler() {
		return scheduler;
	}

	public void setActivityScheduler(PActivityScheduler aScheduler) {
		scheduler = aScheduler;
	}
	
	//****************************************************************
	// Stepping
	//****************************************************************

	/**
	 * Return true if this activity is stepping.
	 */
	public boolean isStepping() {
		return stepping;
	}
	
	/**
	 * Return true if this activity is performing an animation. This is used
	 * by the PCanvas to determine if it should set the render quality to
	 * PCanvas.animatingRenderQuality or not for each frame it renders.
	 */ 
	protected boolean isAnimation() {
		return false;
	}
	
	/**
	 * This method is called right before an activity is scheduled to start
	 * running. After this method is called step() will be called until the
	 * activity finishes.
	 */
	protected void activityStarted() {
		if (delegate != null)
			delegate.activityStarted(this);
	}
	
	/**
	 * This is the method that most activities override to perform their
	 * behavior. It will be called repeatedly when the activity is running.
	 * 
	 * @param elapsedTime the amount of time that has passed relative to the activities startTime.
	 */ 
	protected void activityStep(long elapsedTime) {
		if (delegate != null)
			delegate.activityStepped(this);
	}
	
	/**
	 * This method is called after an activity is has finished running and the
	 * activity has been removed from the PActivityScheduler queue.
	 */
	protected void activityFinished() {
		if (delegate != null)
			delegate.activityFinished(this);
	}
	
	/**
	 * Get the delegate for this activity. The delegate is notified when 
	 * the activity starts and stops stepping.
	 */
	public PActivityDelegate getDelegate() {
		return delegate;
	}

	/**
	 * Set the delegate for this activity. The delegate is notified when 
	 * the activity starts and stops stepping.
	 */
	public void setDelegate(PActivityDelegate delegate) {
		this.delegate = delegate;
	}
		
	//****************************************************************
	// Controlling
	//****************************************************************

	/**
	 * Schedules this activity to start after the first activity has finished.
	 * Note that no link is created between these activities, if the startTime
	 * or duration of the first activity is later changed this activities start
	 * time will not be updated to reflect that change.
	 */	
	public void startAfter(PActivity first) {
		setStartTime(first.getStartTime() + first.getDuration());
	}
	
	/**
	 * Stop this activity immediately, and remove it from the activity
	 * scheduler. The default termination behavior is call activityFinished
	 * if the activity is currently stepping. Use terminate(terminationBehavior)
	 * use a different termination behavior.
	 */
	public void terminate() {
		terminate(TERMINATE_AND_FINISH_IF_STEPPING);
	}

	/**
	 * Stop this activity immediately, and remove it from the activity
	 * scheduler. The termination behavior determines when and if activityStarted
	 * and activityFinished get called. The possible termination behaviors are as
	 * follow:
	 * 
	 * TERMINATE_WITHOUT_FINISHING - The method activityFinished will never get called and
	 * so the activity will be terminated midway.
	 * TERMINATE_AND_FINISH - The method activityFinished will always get called. And so the
	 * activity will always end in it's completed state. If the activity has not yet started
	 * the method activityStarted will also be called.
	 * TERMINATE_AND_FINISH_IF_STEPPING - The method activityFinished will only be called
	 * if the activity has previously started.
	 */
	public void terminate(int terminationBehavior) {
		if (scheduler != null) {
			scheduler.removeActivity(this);
		}
		
		switch (terminationBehavior) {
			case TERMINATE_WITHOUT_FINISHING:
				stepping = false;
				break;
				
			case TERMINATE_AND_FINISH:
				if (stepping) {
					stepping = false;
					activityFinished();
				} else {
					activityStarted();
					activityFinished();
				}

				break;
				
			case TERMINATE_AND_FINISH_IF_STEPPING:
				if (stepping) {
					stepping = false;
					activityFinished();
				}
				break;
		}
	}	
	
	/**
	 * The activity scheduler calls this method and it is here
	 * that the activity decides if it should do a step or not for the
	 * given time.
	 */
	public long processStep(long currentTime) {
		// if before start time
		if (currentTime < startTime) {
			return startTime - currentTime;	 
		}
		
		// if past stop time
		if (currentTime > getStopTime()) {
			if (stepping) {
				stepping = false;
				scheduler.removeActivity(this);
				activityFinished();
			} else {
				activityStarted();
				scheduler.removeActivity(this);
				activityFinished();
			}
			return -1;
		}
		
		// else should be stepping
		if (!stepping) {
			activityStarted();
			stepping = true;
		}

		if (currentTime >= nextStepTime) {
			activityStep(currentTime - startTime);
			nextStepTime = currentTime + stepRate;
		}

		return stepRate;
	}
	
	/**
	 * Return the time when this activity should finish running. At this time
	 * (or soon after) the stoppedStepping method will be called
	 */
	public long getStopTime() {
		if (duration == -1) {
			return Long.MAX_VALUE;
		}
		return startTime + duration;
	}
	
	//****************************************************************
	// Debugging - methods for debugging
	//****************************************************************

	/**
	 * Returns a string representation of this object for debugging purposes.
	 */
	public String toString() {
		String result = super.toString().replaceAll(".*\\.", "");
		return result + "[" + paramString() + "]";
	}

	/**
	 * Returns a string representing the state of this node. This method is
	 * intended to be used only for debugging purposes, and the content and
	 * format of the returned string may vary between implementations. The
	 * returned string may be empty but may not be <code>null</code>.
	 *
	 * @return  a string representation of this node's state
	 */
	protected String paramString() {
		StringBuffer result = new StringBuffer();

		result.append("startTime=" + startTime);
		result.append(",duration=" + duration);
		result.append(",stepRate=" + stepRate);
		if (stepping) result.append(",stepping");
		result.append(",nextStepTime=" + nextStepTime);

		return result.toString();
	}
}
