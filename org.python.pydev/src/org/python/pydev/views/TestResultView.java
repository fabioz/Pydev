/*
 * Created on Sep 27, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.python.pydev.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.pyunit.ITestRunListener;

/**
 * @author ggheorg
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestResultView extends ViewPart {

	/**
	 * @author ggheorg
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	private ITestRunListener listener;
	private static Control control;
	
	public static class Listener implements ITestRunListener {

		private boolean success;
		
		public void testsStarted(int testCount) {
			success = true;
		}

		public void testsFinished() {
			if (success) {
				Display display = control.getDisplay();
				Color green = display.getSystemColor(SWT.COLOR_GREEN);
				control.setBackground(green);
			}
		}

		public void testStarted(String klass, String method) {

		}

		public void testFailed(String klass, String method, String trace) {
			Color red = control.getDisplay().getSystemColor(SWT.COLOR_RED);
			control.setBackground(red);
			success = false;
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		listener = new Listener();
		PydevPlugin.getDefault().addTestListener(listener);
		control = new Label(parent, SWT.NONE);
	}

	public void dispose() {
		if (listener != null) {
			PydevPlugin.getDefault().removeTestListener(listener);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		control.setFocus();

	}
	
	public Control getControl() {
		return control;
	}

	private void changeColor(final boolean success) {
		Display display = getSite().getShell().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				if (control.isDisposed())
					return;
				Display display = control.getDisplay();
				if (success) {
					Color green = display.getSystemColor(SWT.COLOR_GREEN);
					control.setBackground(green);
				} else {
					Color red = display.getSystemColor(SWT.COLOR_RED);
					control.setBackground(red);
				}
			}
		});
	}
	
	public ITestRunListener getListener() {
		return listener;
	}
}
