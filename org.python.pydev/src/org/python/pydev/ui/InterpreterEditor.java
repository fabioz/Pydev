/*
 * Author: atotic
 * Created: Sep 8, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.ui;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.python.copiedfromeclipsesrc.PythonListEditor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * Field editor for a list of python interpreter with executable verifier.
 * 
 * <p>
 * heavily inspired by org.eclipse.jface.preference.PathEditor
 * <p>
 * Tries to run python binary to make sure it exists
 * 
 * Subclasses must implement :<code>parseString</code>,<code>createList</code>,<code>getNewInputObject</code>
 */

public class InterpreterEditor extends PythonListEditor {

    /**
     * The last path, or <code>null</code> if none.
     * It is used so that we can open the editor in the specified place.
     */
    private String lastPath;

	/**
	 * Tree to add libs
	 */
    private Tree tree;
    
    /**
     * Keys are Strings (pointing to executable) 
     * and values are InterpreterInfo.
     */
    private Map executableToInfs = new HashMap();

    private List listControl;

    private Image imageSystemLibRoot;

    private Image imageSystemLib;

    /**
     * Creates a path field editor.
     * 
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    public InterpreterEditor(String labelText, Composite parent) {
        init(IInterpreterManager.INTERPRETER_PATH, labelText);
    	imageSystemLibRoot = PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM_ROOT);
    	imageSystemLib = PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM);
        createControl(parent);
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#createControl(org.eclipse.swt.widgets.Composite)
     */
    protected void createControl(Composite parent) {
        super.createControl(parent);
        listControl = getListControl(parent);
        listControl.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                System.out.println("widgetSelected " + e);
                updateTree();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                System.out.println("widgetDefaultSelected " + e);
                updateTree();
            }

        });
        
    }
    /**
     * Notifies that the Remove button has been pressed.
     */
    protected void removePressed() {
        super.removePressed();
        //we need that because if we stay without any elements, we want to remove the libs...
        updateTree();
    }

    /**
     * @see org.eclipse.jface.preference.ListEditor#doFillIntoGrid(org.eclipse.swt.widgets.Composite, int)
     */
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
    	Composite libsControl = getLibsControl(parent);
    	GridData gd = new GridData(GridData.FILL_BOTH);
    	gd.verticalAlignment = GridData.FILL;
    	gd.horizontalSpan = numColumns;
    	gd.grabExcessHorizontalSpace = true;
    	gd.grabExcessVerticalSpace = true;
    	libsControl.setLayoutData(gd);
    	
//    	Composite composite = new Composite(parent, SWT.NONE);
//    	gd = new GridData(GridData.FILL_VERTICAL);
//    	gd.verticalAlignment = GridData.FILL;
//    	gd.horizontalSpan = numColumns;
//    	gd.grabExcessHorizontalSpace = true;
//    	gd.grabExcessVerticalSpace = true;
//    	composite.setLayoutData(gd);
    }
    
    /**
     * @param parent
     * @return
     */
    private Tree getLibsControl(Composite parent) {
        if (tree == null){
	    	tree = new Tree(parent, SWT.BORDER);
        }
        return tree;
    }

    /**
     * @param listControl
     */
    private void updateTree() {
        if (listControl.getSelectionCount() == 1) {
            String s = listControl.getSelection()[0];
            fillTreeItems(s);
        }else{
            fillTreeItems(null);
        }
    }


    /**
     * @param s
     * 
     */
    private void fillTreeItems(String executable) {
        tree.removeAll();
        
        if(executable != null){
	    	TreeItem item = new TreeItem(tree, SWT.NONE);
	    	item.setText("System libs");
	    	item.setImage(imageSystemLibRoot);
	    	InterpreterInfo info = PydevPlugin.interpreterManager.getInterpreterInfo(executable);
	    	
	    	for (Iterator iter = info.libs.iterator(); iter.hasNext();) {
	            TreeItem subItem = new TreeItem(item, SWT.NONE);
	            subItem.setText((String) iter.next());
	            subItem.setImage(imageSystemLib);
	        }
	    	item.setExpanded(true);
        }
    }


    /**
     * true if executable is jython. A hack,
     */
    static public boolean isJython(String executable) {
        return executable.toLowerCase().indexOf("jython") != -1;
    }


    /** Overriden
     */
    protected String getNewInputObject() {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);

        if (System.getProperty("os.name").startsWith("Win")) {
            dialog.setFilterExtensions(new String[] { "*.exe", "*.*" });
        } else {
            // right file dialog executable filters for unix/mac?
        }

        if (lastPath != null) {
            if (new File(lastPath).exists())
                dialog.setFilterPath(lastPath);
        }

        String file = dialog.open();
        if (file != null) {
            file = file.trim();
            if (file.length() == 0)
                return null;
            lastPath = file;
        }

        if(file!= null){
	        //ok, now that we got the file, let's see if it is valid and get the library info.
            return PydevPlugin.interpreterManager.addInterpreter(file);
        }
        
        return null;
    }

    /** Overriden
     */
    protected String createList(String[] executables) {
        return PydevPlugin.interpreterManager.getStringToPersist(executables);
    }
    
    /** Overriden
     */
    protected String[] parseString(String stringList) {
        return PydevPlugin.interpreterManager.getInterpretersFromPersistedString(stringList);
    }

}
