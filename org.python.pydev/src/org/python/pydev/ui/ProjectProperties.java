/*
 * Author: atotic
 * Created on Mar 11, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.ui;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.editor.codecompletion.PythonShell;
import org.python.pydev.plugin.PydevPlugin;


/**
 * TODO just a stub for now. Volunteers needed to implement real include preferenecs
 */
public class ProjectProperties extends PropertyPage {

	Text pathText;
	public static QualifiedName SRC_PATH_PROP = new QualifiedName(PydevPlugin.getPluginID(), "SOURCE_PATH");
	
	/**
	 * Sample 
	 */
	protected Control createContents(Composite parent) {
		Composite topComp= new Composite(parent, SWT.NONE);
		GridLayout topLayout= new GridLayout();
		topLayout.numColumns= 2;
		topLayout.marginWidth= 0;
		topLayout.marginHeight= 0;
		topComp.setLayout(topLayout);

		
		
		Label pathLabel = new Label(topComp, SWT.NONE);
		pathLabel.setText("PYTHONPATH");
		GridData data = new GridData ();
		data.verticalAlignment = GridData.BEGINNING;
		pathLabel.setLayoutData (data);

		//code to do a table...
//		Table table = new Table(parent, SWT.SINGLE | SWT.H_SCROLL 
//				| SWT.V_SCROLL | SWT.FULL_SELECTION);
//		table.setHeaderVisible(true);
//		table.setLinesVisible(true);
//
//		TableColumn column = new TableColumn(table, SWT.NONE, 0);
//		column.setText("Test");
//		column.setWidth(300);
//		column.setAlignment(SWT.LEFT);
//
//		column = new TableColumn(table, SWT.NONE, 1);
//		column.setText("Time(ms)");
//		column.setWidth(100);
//		column.setAlignment(SWT.RIGHT);
//
		//TableEditor and not viewer...still 
		//TODO:Finish this (let's have a table so that the user can enter the PYTHONPATH)
//		viewer = new TableViewer(table);
//		viewer.setLabelProvider(new TestReportLabelProvider());
//		viewer.setContentProvider(new TestReportContentProvider());
//		viewer.setInput(new ArrayList());

		
		pathText = new Text (topComp, SWT.BORDER);
		data = new GridData ();
		data.grabExcessHorizontalSpace = true;
		pathText.setLayoutData (data);
		
		Button button = new Button(topComp, SWT.NONE);
		button.setText("Restore PYTHONPATH");
		button.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                try {
                    String str = "";
                    
                    List pythonPath = PythonShell.getServerShell(PythonShell.COMPLETION_SHELL).getPythonPath();
                    for (Iterator iter = pythonPath.iterator(); iter.hasNext();) {
                        String[] element = (String[]) iter.next();
                        if(element.length > 0){
                            if(str.length() > 0){
                                str += ", ";
                            }
                            str += element[0];
                        }                        
                    }
                    pathText.setText(str);
                } catch (CoreException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
		    
		});
		
		return topComp;
	}

	/**
	 * Saves values into the project
	 */
	public boolean performOk() {
		IProject project = (IProject)getElement().getAdapter(IProject.class);
		if (project != null) {
			try {
				project.setPersistentProperty(SRC_PATH_PROP, pathText.getText());
			} catch (CoreException e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error setting project properties", e);
			}
		}
		return true;
	}

}
