/*
 * Created on Nov 10, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.python.pydev.views;


import java.util.ArrayList;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.pyunit.TestReportContentProvider;
import org.python.pydev.pyunit.TestReportLabelProvider;

/**
 * @author ggheorg
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestReportView extends ViewPart {

	private TableViewer viewer;
	
	public TableViewer getViewer(){
		return viewer;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		Table table = new Table(parent, SWT.SINGLE | SWT.H_SCROLL 
				| SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn column = new TableColumn(table, SWT.NONE, 0);
		column.setText("Test");
		column.setWidth(300);
		column.setAlignment(SWT.LEFT);

		column = new TableColumn(table, SWT.NONE, 1);
		column.setText("Time(ms)");
		column.setWidth(100);
		column.setAlignment(SWT.RIGHT);

		viewer = new TableViewer(table);
		viewer.setLabelProvider(new TestReportLabelProvider());
		viewer.setContentProvider(new TestReportContentProvider());
		viewer.setInput(new ArrayList());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		viewer.getControl().setFocus();

	}

}
