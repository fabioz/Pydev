/*
 * Author: atotic
 * Created: Sep 8, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.python.copiedfromeclipsesrc.PythonListEditor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.IInterpreterManager;
import org.python.pydev.ui.UIConstants;

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
     * Interpreter manager we are using (given at init)
     */
    private IInterpreterManager interpreterManager;
    
	/**
	 * Tree to add libs.
	 */
    private Tree tree;
    
    /**
     * Keys are Strings (pointing to executable) 
     * and values are InterpreterInfo.
     */
    private Map executableToInfs = new HashMap();

    /**
     * This is the control where the interpreters are shown
     */
    private List listControl;

    /**
     * Images
     */
    private Image imageSystemLibRoot;

    /**
     * Images
     */
    private Image imageSystemLib;

    private Composite box;

    private Button addBtOthers;

    private Button removeBtOthers;

    private SelectionListener selectionListenerOthers;

    private List listBuiltins;

    public List getExesList(){
        return listControl;
    }
    
    /**
     * Creates a path field editor.
     * 
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    public InterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        init(IInterpreterManager.INTERPRETER_PATH, labelText);
        this.interpreterManager = interpreterManager;
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
                updateTree();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                updateTree();
            }

        });
    }

    /**
     * @param parent
     * @return
     */
    private Tree getTreeLibsControl(Composite parent) {
        if (tree == null){
	    	tree = new Tree(parent, SWT.BORDER);
	    	tree.setFont(parent.getFont());
			tree.addDisposeListener(new DisposeListener() {
			    public void widgetDisposed(DisposeEvent event) {
			        tree = null;
			    }
			});
        }
        return tree;
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

    	Label l1 = new Label(parent, SWT.None);
    	l1.setText("System PYTHONPATH");
    	GridData gd = new GridData();
    	gd.horizontalSpan = numColumns;
    	gd.grabExcessHorizontalSpace = false;
    	gd.grabExcessVerticalSpace = false;
    	l1.setLayoutData(gd);

    	
    	Composite control = getTreeLibsControl(parent);
    	gd = new GridData(GridData.FILL_BOTH);
    	gd.horizontalSpan = numColumns;
    	gd.grabExcessHorizontalSpace = true;
    	gd.grabExcessVerticalSpace = true;
    	control.setLayoutData(gd);

    	
    	Label l2 = new Label(parent, SWT.None);
    	l2.setText("Forced builtin libs (check http://pydev.sf.net/faq.html for more info).");
    	gd = new GridData();
    	gd.horizontalSpan = numColumns;
    	gd.grabExcessHorizontalSpace = false;
    	gd.grabExcessVerticalSpace = false;
    	l2.setLayoutData(gd);

    	
    	List list = getBuiltinsListControl(parent);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = true;
        list.setLayoutData(gd);
    	
    	control = getButtonBoxControlOthers(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        control.setLayoutData(gd);
    }
    /**
     * @param parent
     * @return
     */
    private List getBuiltinsListControl(Composite parent) {
        if (listBuiltins == null) {
            listBuiltins = new List(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            listBuiltins.setFont(parent.getFont());
            listBuiltins.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    listBuiltins = null;
                }
            });
        } else {
            checkParent(listBuiltins, parent);
        }
        return listBuiltins;
    }

    /**
     * Returns this field editor's button box containing the Add, Remove, Up, and Down button.
     * 
     * @param parent the parent control
     * @return the button box
     */
    public Composite getButtonBoxControlOthers(Composite parent) {
        if (box == null) {
            box = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            box.setLayout(layout);
            addBtOthers = createBt(box, "ListEditor.add");//$NON-NLS-1$
            removeBtOthers = createBt(box, "ListEditor.remove");//$NON-NLS-1$
            box.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    addBtOthers = null;
                    removeBtOthers = null;
                    box = null;
                }
            });

        } else {
            checkParent(box, parent);
        }

        return box;
    }
    /**
     * Returns this field editor's selection listener. The listener is created if nessessary.
     * 
     * @return the selection listener
     */
    private SelectionListener getSelectionListenerOthers() {
        if (selectionListenerOthers == null){
            selectionListenerOthers = new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    Widget widget = event.widget;
                    if (widget == addBtOthers) {
                        addOthers();
                    } else if (widget == removeBtOthers) {
                        removeOthers();
                    }
                }
            };
        }
        return selectionListenerOthers;
    }

    /**
     * 
     */
    protected void addOthers() {
        if (listControl.getSelectionCount() == 1) {
            String executable = listControl.getSelection()[0];
	        InterpreterInfo info = interpreterManager.getInterpreterInfo(executable, new NullProgressMonitor());
	        
	        InputDialog d = new InputDialog(this.getShell(), "Builtin to add", "Builtin to add", "", null);
	        
	        int retCode = d.open();
	        if (retCode == InputDialog.OK) {
		        info.forcedLibs.add(d.getValue());
	        }

        }
        updateTree();
    }

    /**
     * 
     */
    protected void removeOthers() {
        if (listControl.getSelectionCount() == 1 && listBuiltins.getSelectionCount() == 1) {
            String executable = listControl.getSelection()[0];
            String builtin = listBuiltins.getSelection()[0];
            
	        InterpreterInfo info = interpreterManager.getInterpreterInfo(executable, new NullProgressMonitor());
	        info.forcedLibs.remove(builtin);
        }
        updateTree();
    }

    /**
     * Helper method to create a push button.
     * 
     * @param parent the parent control
     * @param key the resource name used to supply the button's label text
     * @return Button
     */
    private Button createBt(Composite parent, String key) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(JFaceResources.getString(key));
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.heightHint = convertVerticalDLUsToPixels(button, IDialogConstants.BUTTON_HEIGHT);
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        button.addSelectionListener(getSelectionListenerOthers());
        return button;
    }


    /**
     * @param listControl
     */
    private void updateTree() {
        if (listControl.getSelectionCount() == 1) {
            String s = listControl.getSelection()[0];
            fillPathItems(s);
        }else{
            fillPathItems(null);
        }
    }


    /**
     * @param s
     * 
     */
    private void fillPathItems(String executable) {
        tree.removeAll();
        listBuiltins.removeAll();
        
        if(executable != null){
	    	TreeItem item = new TreeItem(tree, SWT.NONE);
	    	item.setText("System libs");
	    	item.setImage(imageSystemLibRoot);

	    	InterpreterInfo info = interpreterManager.getInterpreterInfo(executable, new NullProgressMonitor());
	    	
	    	for (Iterator iter = info.libs.iterator(); iter.hasNext();) {
	            TreeItem subItem = new TreeItem(item, SWT.NONE);
	            subItem.setText((String) iter.next());
	            subItem.setImage(imageSystemLib);
	        }
	    	item.setExpanded(true);
	    	
	    	//ok, now set the dlls
	    	item = new TreeItem(tree, SWT.NONE);
	    	item.setText("Compiled libs found in PYTHONPATH (dlls)");
	    	item.setImage(imageSystemLibRoot);

	    	for (Iterator iter = info.dllLibs.iterator(); iter.hasNext();) {
	            TreeItem subItem = new TreeItem(item, SWT.NONE);
	            subItem.setText((String) iter.next());
	            subItem.setImage(imageSystemLib);
	        }
	    	item.setExpanded(false);
	    	
	    	
	    	//set the forced builtins
	    	for (Iterator iter = info.forcedLibs.iterator(); iter.hasNext();) {
	    	    listBuiltins.add((String) iter.next());
            }
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
            ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(this.getShell());
            monitorDialog.setBlockOnOpen(false);
            try {
                Operation operation = new Operation(file);
                monitorDialog.run(true, false, operation);
                
                if(operation.e != null){
                    throw operation.e;
                }
                
                return operation.result;
            } catch (Exception e) {
                PydevPlugin.log(e);
                return null;
            }
        }
        
        return null;
    }

    /** Overriden
     */
    protected String createList(String[] executables) {
        return interpreterManager.getStringToPersist(executables);
    }
    
    /** Overriden
     */
    protected String[] parseString(String stringList) {
        return interpreterManager.getInterpretersFromPersistedString(stringList);
    }

    private class Operation implements IRunnableWithProgress{

        public String result;
        public String file;
        public Exception e;
        
        /**
         * @param file2
         */
        public Operation(String file2) {
            this.file = file2;
        }

        /**
         * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Getting libs", 100);
            try {
                result = interpreterManager.addInterpreter(file, monitor);
            } catch (Exception e) {
                this.e = e;
            }
            monitor.done();
        }
        
    }
    
    /**
     * @see org.python.copiedfromeclipsesrc.PythonListEditor#doLoadDefault()
     */
    protected void doLoadDefault() {
        super.doLoadDefault();
        updateTree();
    }
}
