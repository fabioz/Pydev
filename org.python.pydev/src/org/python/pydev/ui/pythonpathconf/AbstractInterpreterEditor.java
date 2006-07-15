/*
 * Author: atotic
 * Created: Sep 8, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.python.copiedfromeclipsesrc.PythonListEditor;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;
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

public abstract class AbstractInterpreterEditor extends PythonListEditor {

    public static boolean USE_ICONS = true;
    
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

    boolean changed = false;

    private Composite boxSystem;

    private Button addBtSystemFolder;

    private Button removeBtSystemFolder;

    private Button addBtSystemJar;

    private SelectionListener selectionListenerSystem;

    public List getExesList(){
        return listControl;
    }
    
    /**
     * Creates a path field editor linked to the preference name passed
     * 
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    protected AbstractInterpreterEditor(String preferenceName, String labelText, Composite parent, IInterpreterManager interpreterManager) {
        init(preferenceName, labelText);
        this.interpreterManager = interpreterManager;
        if(USE_ICONS){
            imageSystemLibRoot = PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM_ROOT);
            imageSystemLib = PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM);
        }
        createControl(parent);
        updateTree();
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
        updateTree();
        changed = true;
        //we need that because when the user remove something, we want to remove the cache for that.
        this.store();
    }

    protected void addPressed() {
        super.addPressed();
        updateTree();
        changed = true;
    }

    protected void upPressed() {
        super.upPressed();
        changed = true;
    }
    
    protected void downPressed() {
        super.downPressed();
        changed = true;
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
    	l1.setLayoutData(gd);

    	//the tree
        Tree tree = getTreeLibsControl(parent);
    	gd = new GridData();
    	gd.horizontalSpan = numColumns - 1;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        tree.setLayoutData(gd);

        //buttons at the side of the tree
        Composite control = getButtonBoxControlSystem(parent);
    	gd = new GridData();
    	gd.verticalAlignment = GridData.BEGINNING;
    	gd.horizontalSpan = numColumns - 1;
    	control.setLayoutData(gd);

        //label
    	Label l2 = new Label(parent, SWT.None);
    	l2.setText("Forced builtin libs (check http://pydev.sf.net/faq.html for more info).");
    	gd = new GridData();
    	gd.horizontalSpan = numColumns;
    	l2.setLayoutData(gd);

        //the list with the builtins
    	List list = getBuiltinsListControl(parent);
        gd = new GridData();
        gd.horizontalSpan = numColumns - 1;
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = false;
        gd.heightHint = 100;
        list.setLayoutData(gd);
    	
        //the builtins buttons
    	control = getButtonBoxControlOthers(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        gd.horizontalSpan = numColumns - 1;
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
     * Returns this field editor's button box containing the Add Source Folder, Add Jar and Remove
     * 
     * @param parent the parent control
     * @return the button box
     */
    public Composite getButtonBoxControlSystem(Composite parent) {
        if (boxSystem == null) {
            boxSystem = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            boxSystem.setLayout(layout);
            addBtSystemFolder = createBt(boxSystem, "New Folder", getSelectionListenerSystem());//$NON-NLS-1$
            if(this.interpreterManager.isJython()){
                addBtSystemJar = createBt(boxSystem, "New Jar", getSelectionListenerSystem());//$NON-NLS-1$
            }
            removeBtSystemFolder = createBt(boxSystem, "ListEditor.remove", getSelectionListenerSystem());//$NON-NLS-1$
            boxSystem.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    addBtSystemJar = null;
                    addBtSystemFolder = null;
                    removeBtSystemFolder = null;
                    boxSystem = null;
                }
            });
            
        } else {
            checkParent(boxSystem, parent);
        }
        
        return boxSystem;
    }
    
    /**
     * Returns this field editor's button box containing the Add and Remove
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
            addBtOthers = createBt(box, "ListEditor.add", getSelectionListenerOthers());//$NON-NLS-1$
            removeBtOthers = createBt(box, "ListEditor.remove", getSelectionListenerOthers());//$NON-NLS-1$
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

    private static String lastDirectoryDialogPath = null;
    private static String lastFileDialogPath = null;
    
    /**
     * Returns this field editor's selection listener. The listener is created if nessessary.
     * 
     * @return the selection listener
     */
    private SelectionListener getSelectionListenerSystem() {
        if (selectionListenerSystem == null){
            selectionListenerSystem = new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    if (listControl.getSelectionCount() == 1) {
                        String executable = listControl.getSelection()[0];
                        InterpreterInfo info = (InterpreterInfo) interpreterManager.getInterpreterInfo(executable, new NullProgressMonitor());

                    
                        Widget widget = event.widget;
                        if (widget == addBtSystemFolder) {
                            DirectoryDialog dialog = new DirectoryDialog(getShell());
                            dialog.setFilterPath(lastDirectoryDialogPath);
                            String filePath = dialog.open();
                            if(filePath != null){
                                lastDirectoryDialogPath = filePath;
                                info.libs.add(filePath);
                                changed = true;
                            }
                            
                        } else if (widget == addBtSystemJar) {
                            FileDialog dialog = new FileDialog(getShell());
                            dialog.setFilterPath(lastFileDialogPath);
                            String filePath = dialog.open();
                            if(filePath != null){
                                lastFileDialogPath = filePath;
                                info.libs.add(filePath);
                                info.dllLibs.add(filePath);
                                changed = true;
                            }
                                
                        } else if (widget == removeBtSystemFolder) {
                            TreeItem[] selection = tree.getSelection();
                            for (int i = 0; i < selection.length; i++) {
                                TreeItem s = selection[i];
                                String text = s.getText();
                                info.libs.remove(text);
                                info.dllLibs.remove(text);
                                changed = true;
                            }
                        }
                        updateTree();
                    }
                }
            };
        }
        return selectionListenerSystem;
    }
    
    /**
     * 
     */
    protected void addOthers() {
        if (listControl.getSelectionCount() == 1) {
            String executable = listControl.getSelection()[0];
	        InterpreterInfo info = (InterpreterInfo) interpreterManager.getInterpreterInfo(executable, new NullProgressMonitor());
	        
	        InputDialog d = new InputDialog(this.getShell(), "Builtin to add", "Builtin to add", "", null);
	        
	        int retCode = d.open();
	        if (retCode == InputDialog.OK) {
		        info.forcedLibs.add(d.getValue());
                changed = true;
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
            
	        InterpreterInfo info = (InterpreterInfo) interpreterManager.getInterpreterInfo(executable, new NullProgressMonitor());
	        info.forcedLibs.remove(builtin);
	        changed = true;
        }
        updateTree();
    }

    /**
     * Helper method to create a push button.
     * 
     * @param parent the parent control
     * @param key the resource name used to supply the button's label text
     * @param listenerToAdd 
     * @return Button
     */
    private Button createBt(Composite parent, String key, SelectionListener listenerToAdd) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(JFaceResources.getString(key));
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
//        data.heightHint = convertVerticalDLUsToPixels(button, IDialogConstants.BUTTON_HEIGHT);
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        button.addSelectionListener(listenerToAdd);
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
            if (listControl.getItemCount() > 0){
                listControl.select(0);
                selectionChanged();
                String s = listControl.getSelection()[0];
                fillPathItems(s);
            }
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

	    	InterpreterInfo info = (InterpreterInfo) interpreterManager.getInterpreterInfo(executable, new NullProgressMonitor());
	    	
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
     * @return a string with the extensions that are accepted for the interpreter
     */
    public abstract String[] getInterpreterFilterExtensions();
    
    /** Overriden
     */
    protected String getNewInputObject() {
    	CharArrayWriter charWriter = new CharArrayWriter();
    	PrintWriter logger = new PrintWriter(charWriter);
    	logger.println("Information about process of adding new interpreter:");
        try {
        	
			FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);

			String[] filterExtensions = getInterpreterFilterExtensions();
			if (filterExtensions != null) {
				dialog.setFilterExtensions(filterExtensions);
			}

			if (lastPath != null) {
				if (new File(lastPath).exists())
					dialog.setFilterPath(lastPath);
			}

			logger.println("- Opening dialog to request executable (or jar).");
			String file = dialog.open();
			if (file != null) {
				logger.println("- Chosen interpreter file:'"+file);
				file = file.trim();
				if (file.length() == 0){
					logger.println("- When trimmed, the chosen file was empty (returning null).");
					return null;
				}
				lastPath = file;
			}else{
				logger.println("- The file chosen was null (returning null).");
				return null;
			}
			
			if (file != null) {
				//ok, now that we got the file, let's see if it is valid and get the library info.
				logger.println("- Ok, file is non-null. Getting info on:"+file);
				ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(this.getShell());
				monitorDialog.setBlockOnOpen(false);
				Operation operation = new Operation(file, logger);
				monitorDialog.run(true, false, operation);

				if (operation.e != null) {
					logger.println("- Some error happened while getting info on the interpreter:");
					operation.e.printStackTrace(logger);
					throw operation.e;
				}

				logger.println("- Success getting the info. Result:"+operation.result);
				return operation.result;
			}
			
        } catch (Exception e) {
            PydevPlugin.log(e);
            return null;
        } finally {
        	PydevPlugin.logInfo(charWriter.toString());
        }
        
        return null;
    }

    @Override
    protected void doStore() {
        String s = createList(list.getItems());
        if (s != null){
        	interpreterManager.setPersistedString(s);
        }
    }
    
    @Override
    protected void doLoad() {
        if (list != null) {
        	String s = interpreterManager.getPersistedString();
            String[] array = parseString(s);
            for (int i = 0; i < array.length; i++) {
                list.add(array[i]);
            }
        }
        updateTree();
    }

    public String getPreferenceName(){
    	throw new RuntimeException("The preferences should be stored/gotten from the IInterpreterManager, and not directly.");
    }
    
    /** Overriden
     */
    @Override
    protected String createList(String[] executables) {
        return interpreterManager.getStringToPersist(executables);
    }
    
    /** Overriden
     */
    @Override
    protected String[] parseString(String stringList) {
        return interpreterManager.getInterpretersFromPersistedString(stringList);
    }

    private static class OperationMonitor extends ProgressMonitorWrapper{

		private PrintWriter logger;

		protected OperationMonitor(IProgressMonitor monitor, PrintWriter logger) {
			super(monitor);
			this.logger = logger;
		}
    	
		@Override
		public void beginTask(String name, int totalWork) {
			super.beginTask(name, totalWork);
			logger.print("- Beggining task:");
			logger.print(name);
			logger.print(" totalWork:");
			logger.println(totalWork);
		}
		
		@Override
		public void setTaskName(String name) {
			super.setTaskName(name);
			logger.print("- Setting task name:");
			logger.println(name);
		}
		
		@Override
		public void subTask(String name) {
			super.subTask(name);
			logger.print("- Sub Task:");
			logger.println(name);
		}
    }
    private class Operation implements IRunnableWithProgress{

        public String result;
        public String file;
        public Exception e;
		private PrintWriter logger;
        
        /**
         * @param file2
         * @param logger 
         */
        public Operation(String file2, PrintWriter logger) {
            this.file = file2;
            this.logger = logger;
        }

        /**
         * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        	monitor = new OperationMonitor(monitor, logger);
            monitor.beginTask("Getting libs", 100);
            try {
                result = interpreterManager.addInterpreter(file, monitor);
            } catch (Exception e) {
            	logger.println("Exception detected: "+e.getMessage());
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

    public boolean hasChanged() {
        return changed;
    }
}
