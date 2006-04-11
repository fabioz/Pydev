/*
 * Copyright (C) 2002-@year@ by University of Maryland, College Park, MD 20742, USA
 * All rights reserved.
 *
 * Piccolo was written at the Human-Computer Interaction Laboratory
 * www.cs.umd.edu/hcil by Jesse Grosjean under the supervision of Ben Bederson.
 * The Piccolo website is www.cs.umd.edu/hcil/piccolo
 */
package edu.umd.cs.piccolox.swt;

import java.awt.event.ActionListener;
import javax.swing.Timer;

import org.eclipse.swt.widgets.Composite;

import edu.umd.cs.piccolo.PRoot;

/**
 * <b>PSWTRoot</b> is a subclass of PRoot that is designed to work in the SWT
 * environment. In particular it uses SWTTimers and the SWT event dispatch
 * thread. With the current setup only a single PSWTCanvas is expected to be
 * connected to a root.
 * <P>
 * @version 1.1
 * @author Jesse Grosjean
 */
public class PSWTRoot extends PRoot {

	private Composite composite;
	
	public PSWTRoot(Composite composite) {
		this.composite = composite;
	}

	public Timer createTimer(int delay, ActionListener listener) {
		return new SWTTimer(composite.getDisplay(),delay,listener);
	}

	public void scheduleProcessInputsIfNeeded() {
		if (!Thread.currentThread().equals(composite.getDisplay().getThread())) {
			return;
		}

		if (!processInputsScheduled && !processingInputs &&
			(getFullBoundsInvalid() || getChildBoundsInvalid() || getPaintInvalid() || getChildPaintInvalid())) {

			processInputsScheduled = true;
			composite.getDisplay().asyncExec(new Runnable() {
				public void run() {
					processInputs();
					processInputsScheduled = false;
				}
			});
		}
	}
}
