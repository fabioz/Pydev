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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

import edu.umd.cs.piccolo.PRoot;
import edu.umd.cs.piccolo.util.PUtil;

/**
 * <b>PActivityScheduler</b> is responsible for maintaining a list of
 * activities. It is given a chance to process these activities from 
 * the PRoot's processInputs() method. Most users will not need to use
 * the PActivityScheduler directly, instead you should look at:
 * <ul>
 * <li>PNode.addActivity - to schedule a new activity 		
 * <li>PActivity.terminate - to terminate a running activity 		
 * <li>PRoot.processInputs - already calls processActivities for you.
 * </ul>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PActivityScheduler {
		
	private PRoot root;
	private List activities;
	private Timer activityTimer;
	private boolean activitiesChanged;
	private boolean animating;	
	private ArrayList processingActivities;

	public PActivityScheduler(PRoot rootNode) {
		root = rootNode;
		activities = new ArrayList();
		processingActivities = new ArrayList();
	}
		
	public PRoot getRoot() {
		return root;
	}
	
	public void addActivity(PActivity activity) {
		addActivity(activity, false);
	}

	/**
	 * Add this activity to the scheduler. Sometimes it's useful to make sure
	 * that an activity is run after all other activities have been run. To do
	 * this set processLast to true when adding the activity.
	 */
	public void addActivity(PActivity activity, boolean processLast) {
		if (activities.contains(activity)) return;

		activitiesChanged = true;
		
		if (processLast) {
			activities.add(0, activity);
		} else {
			activities.add(activity);
		}

		activity.setActivityScheduler(this);

		if (!getActivityTimer().isRunning()) {
			startActivityTimer();
		}		
	}
		
	public void removeActivity(PActivity activity) {
		if (!activities.contains(activity)) return;

		activitiesChanged = true;
		activities.remove(activity);

		if (activities.size() == 0) {
			stopActivityTimer();
		}					
	}

	public void removeAllActivities() {		
		activitiesChanged = true;	
		activities.clear();
		stopActivityTimer();
	}

	public List getActivitiesReference() {
		return activities;
	}
	
	/**
	 * Process all scheduled activities for the given time. Each activity
	 * is given one "step", equivalent to one frame of animation.
	 */	
	public void processActivities(long currentTime) {
		int size = activities.size();		
		if (size > 0) {
			processingActivities.clear();
			processingActivities.addAll(activities);
			for (int i = size - 1; i >= 0; i--) {
				PActivity each = (PActivity) processingActivities.get(i);
				each.processStep(currentTime);
			}
		}		
	}
		
	/**
	 * Return true if any of the scheduled activities return true to
	 * the message isAnimation();
	 */
	public boolean getAnimating() {
		if (activitiesChanged) {
			animating = false;
			for(int i=0; i<activities.size(); i++) {
				PActivity each = (PActivity) activities.get(i);
				animating |= each.isAnimation();
			}	
			activitiesChanged = false;
		}
		return animating;
	}
			
	protected void startActivityTimer() {
		getActivityTimer().start();
	}
	
	protected void stopActivityTimer() {
		getActivityTimer().stop();
	}
		
	protected Timer getActivityTimer() {
		if (activityTimer == null) {
			activityTimer = root.createTimer(PUtil.ACTIVITY_SCHEDULER_FRAME_DELAY, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					root.processInputs();
				}
			});
		}
		return activityTimer;
	}
}

