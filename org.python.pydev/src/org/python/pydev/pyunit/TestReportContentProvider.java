/*
 * Created on Nov 10, 2004
 *
 */
package org.python.pydev.pyunit;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author ggheorg
 *
 */
public class TestReportContentProvider implements IStructuredContentProvider,
		ITestRunListener {

	private TableViewer viewer;
	private TestResult currentResult;
	
	public TestReportContentProvider() {
		PydevPlugin.getDefault().addTestListener(this);
	}

	public void dispose() {
		PydevPlugin.getDefault().removeTestListener(this);
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
	public void testsStarted(int testCount) {
		((List)viewer.getInput()).clear();
		currentResult = null;
		viewer.refresh();
	}


	public void testsFinished() {
		addLastResult();
	}


	public void testStarted(String klass, String method) {
		addLastResult();
		currentResult = new TestResult(klass, method, 0, System.currentTimeMillis());
	}


	public void testFailed(String klass, String method, String trace) {
		currentResult.testFailed();
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
					if (!viewer.getControl().isDisposed())
						viewer.insert(currentResult, -1);
				}
			});
		}
	}
}
