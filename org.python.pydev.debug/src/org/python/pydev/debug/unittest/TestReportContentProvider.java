/*
 * Created on Nov 10, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.python.pydev.debug.unittest;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * @author ggheorg
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestReportContentProvider implements IStructuredContentProvider,
		ITestRunListener {

	private TableViewer viewer;
	private TestResult currentResult;
	private String testFile;
	
	public TestReportContentProvider() {
		PydevDebugPlugin.getDefault().addTestListener(this);
	}

	public void dispose() {
		PydevDebugPlugin.getDefault().removeTestListener(this);
	}
	
	public Object[] getElements(Object inputElement) {
		return ((List)inputElement).toArray();
	}

	private List getTestResults() {
		return (List)viewer.getInput();
	}
	
	public void inputChanged(Viewer tableViewer, Object oldInput, 
			Object newInput) {
		viewer = (TableViewer)tableViewer;
		
	}
	public void testsStarted(int testCount, String testFile) {
		this.testFile = testFile;
		((List)viewer.getInput()).clear();
		currentResult = null;
		Control ctrl = viewer.getControl();
		if (ctrl == null || ctrl.isDisposed())
			return;
		ctrl.getDisplay().syncExec(new Runnable() {
			public void run() {
				if (!viewer.getControl().isDisposed())
					viewer.refresh();
			}
		});
	}


	public void testsFinished(String summary) {
		addLastResult();
	}


	public void testStarted(String klass, String method) {
		addLastResult();
		currentResult = new TestResult(testFile, klass, method, 0, System.currentTimeMillis());
	}

	public void testOK(String klass, String method) {
	}

	public void testFailed(String klass, String method, String failureType, String trace) {
		currentResult.testFailed(failureType);
	}

	private void addLastResult() {
		if (currentResult != null) {
			currentResult.testFinished();
			getTestResults().add(currentResult);
			Control ctrl = viewer.getControl();
			if (ctrl == null || ctrl.isDisposed())
				return;
			ctrl.getDisplay().syncExec(new Runnable() {
				public void run() {
					int lineIndex = currentResult.isFailure() ? 0 : -1; 
					if (!viewer.getControl().isDisposed())
						viewer.insert(currentResult, lineIndex);
				}
			});
		}
	}
}
