/*
 * Author: Fabio Zadrozny
 * Created on Mar 11, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.editor.codecompletion.PythonShell;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;


/**
 * 
 * This page is specified to appear in the plugin.xml
 */
public class PyProjectProperties extends PropertyPage {

    /**
     * This is the property that has the python path - associated with the project.
     */
	public static QualifiedName PYTHONPATH_PROP = new QualifiedName(PydevPlugin.getPluginID(), "SOURCE_PATH");
	
    /**
     * @return
     */
    public static List getProjectPythonPath(IProject project) {
        List paths;
        try {
            String persistentProperty = getProjectPythonPathStr(project);

            if(persistentProperty != null){
                String[] strings = persistentProperty.split("\\|");
        	
                paths = new ArrayList(Arrays.asList(strings));
            }else{
                paths = new ArrayList();
            }
        } catch (Exception e) {

            PydevPlugin.log(e);
            paths = new ArrayList();
            
        }
        return paths;
    }

    /**
     * @param project
     * @return
     * @throws CoreException
     */
    public static String getProjectPythonPathStr(IProject project) throws CoreException {
        String persistentProperty = project.getPersistentProperty(PYTHONPATH_PROP);
        return persistentProperty;
    }

    /**
     * This is the project we are editing
     */
    private IProject project;
    
    /**
     * This is the table to edit the pythonpath
     */
    private Table table;

    /**
     * This is the table editor
     */
    private TableEditor editor;
	
	/**
	 * Creates contents given its parent.
	 */
	protected Control createContents(Composite p) {
		project = (IProject)getElement().getAdapter(IProject.class);
		
        Composite topComp= new Composite(p, SWT.NONE);
        GridLayout innerLayout= new GridLayout();
        innerLayout.numColumns= 2;
        innerLayout.marginHeight= 0;
        innerLayout.marginWidth= 0;
        topComp.setLayout(innerLayout);
        GridData gd= new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan= 2;
        topComp.setLayoutData(gd);
        
        
		GridData data = new GridData ();

		if(project != null){
	
			//code to do a table...
			table = new Table(topComp, SWT.H_SCROLL 
					| SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.BORDER | SWT.MULTI);
			table.setHeaderVisible(true);
			table.setLinesVisible(true);
	
			
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText("Path");
			column.setWidth(300);
			column.setAlignment(SWT.LEFT);
	
			List paths = getProjectPythonPath(project);
			
        	fillTableWithPath(paths);
			
			createTableEditor();
			
			data= new GridData(GridData.FILL_BOTH);
			data.widthHint= convertWidthInCharsToPixels(3);
			data.heightHint= convertHeightInCharsToPixels(10);
			table.setLayoutData(data);

			TableLayout tableLayout= new TableLayout();
			table.setLayout(tableLayout);

			
			
			// ----------------------- buttons
			Composite buttons= new Composite(topComp, SWT.NONE);
			buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
			GridLayout layout = new GridLayout();
			layout.marginHeight= 0;
			layout.marginWidth= 0;
			buttons.setLayout(layout);
		
			Button button = new Button(buttons, SWT.PUSH);
			customizeRestorePythonPathButton(button);

			Button buttonAdd = new Button(buttons, SWT.PUSH);
            customizeAddButton(buttonAdd);

			
			Button buttonRem = new Button(buttons, SWT.PUSH);
			customizeRemButton(buttonRem);
		}
		return topComp;
	}

	/**
     * Set actions for button and layout.
     * @param buttonRem
     */
    private void customizeRemButton(Button buttonRem) {
        GridData data;
        buttonRem.setText("Remove Item");
        buttonRem.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                //when deleting, first dispose editor
                Control oldEd = editor.getEditor();
                if(oldEd != null) oldEd.dispose();

                int[] indices = table.getSelectionIndices();
                Arrays.sort(indices); //just make sure it is sorted correctly
                for (int i = indices.length-1; i >= 0; i--) {
                    table.remove(indices[i]);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
            
        });
        data = new GridData ();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        buttonRem.setLayoutData(data);
    }

    /**
     * Set actions for button and layout.
     * 
     * @param buttonAdd
     */
    private void customizeAddButton(final Button buttonAdd) {
        GridData data;
        buttonAdd.setText("Add Item");
        buttonAdd.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(buttonAdd.getShell());
                String path = dialog.open();
                if(path != null){
	        		TableItem item = new TableItem(table, SWT.NONE);
	        		item.setText(new String[] {path});
                }                        

            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
            
        });
        data = new GridData ();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        buttonAdd.setLayoutData(data);
    }

    /**
     * Appends the paths given to the table
     * 
     * @param paths
     */
    private void fillTableWithPath(List paths) {
        for (Iterator iter = paths.iterator(); iter.hasNext();) {
            String element = (String) iter.next();
        	TableItem item = new TableItem(table, SWT.NONE);
        	item.setText(new String[] {element});
        }
    }


    /**
     * Creates editor for the table.
     */
    private void createTableEditor() {
        editor = new TableEditor(table);
        editor.horizontalAlignment = SWT.LEFT;
        editor.grabHorizontal = true;
        editor.minimumWidth = 50;
        // editing the first column
        final int EDITABLECOLUMN = 0;
        
        table.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		// Clean up any previous editor control
        		Control oldEditor = editor.getEditor();
        		if (oldEditor != null) oldEditor.dispose();
        
        		//if more than one is selected, let's make the multiple selection instead of 
        		//making it editable
        		if(table.getSelectionCount() == 1){
	        		// Identify the selected row
	        		TableItem item = (TableItem)e.item;
	        		if (item == null) return;
	        
	        		// The control that will be the editor must be a child of the Table
	        		Text newEditor = new Text(table, SWT.NONE);
	        		newEditor.setText(item.getText(EDITABLECOLUMN));
	        		newEditor.addModifyListener(new ModifyListener() {
	        			public void modifyText(ModifyEvent e) {
	        				Text text = (Text)editor.getEditor();
	        				editor.getItem().setText(EDITABLECOLUMN, text.getText());
	        			}
	        		});
	        		newEditor.selectAll();
	        		newEditor.setFocus();
	        		editor.setEditor(newEditor, item, EDITABLECOLUMN);
	        	}
        	}
        });
    }

    /**
	 * Creates button that when clicked gets the pythonpath from the server shell.
	 * 
     * @param button
     */
    private void customizeRestorePythonPathButton(Button button) {
        button.setText("Restore PYTHONPATH");
        button.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                try {
                    Set p = new HashSet();
                    
                    List pythonPath = PythonShell.getServerShell(PythonShell.COMPLETION_SHELL).getPythonPath();
                    for (Iterator iter = pythonPath.iterator(); iter.hasNext();) {
                        String[] element = (String[]) iter.next();
                        if(element.length > 0){
                            File file = new File(element[0]);
                            if(file.exists() && file.isDirectory()){
                                p.add(element[0]);
                            }
                        }                        
                    }
                    
                    table.removeAll();
                    
                    fillTableWithPath(new ArrayList(p));

                } catch (CoreException e1) {
                    PydevPlugin.log(e1);
                } catch (IOException e1) {
                    PydevPlugin.log(e1);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
            
        });
		GridData data = new GridData ();
		data.verticalAlignment = GridData.END;
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		button.setLayoutData(data);

    }

    /**
     * Apply only saves the new value. does not do code completion update.
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    protected void performApply() {
		doIt(false);
    }
    
    /**
	 * Saves values into the project and updates the code completion. 
	 */
	public boolean performOk() {
		return doIt(true);
	}

    /**
     * Save the pythonpath - only updates model if asked to.
     * @return
     */
    private boolean doIt(boolean updatePath) {
        if (project != null) {
			try {
			    String s = "";
			    TableItem[] items = table.getItems();
			    for (int i = 0; i < items.length; i++) {
			        TableItem item = items[i];
			        
			        String element = item.getText(0);
			        if(element.trim().length() > 0){
			            s += element+ "|";
			        }
                }
				project.setPersistentProperty(PYTHONPATH_PROP, s);
				IProjectNature nature = project.getNature(PythonNature.PYTHON_NATURE_ID);

				if(updatePath){
					if(nature instanceof PythonNature){
					    ((PythonNature)nature).rebuildPath(s);
					}
				}
				
			} catch (Exception e) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error setting project properties", e);
			}
		}
		return true;
    }

	
}
