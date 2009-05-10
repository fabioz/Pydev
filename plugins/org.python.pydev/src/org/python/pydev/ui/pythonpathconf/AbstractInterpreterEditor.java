/*
 * Author: atotic
 * Created: Sep 8, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.copiedfromeclipsesrc.PythonListEditor;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.ui.UIConstants;
import org.python.pydev.ui.dialogs.InterpreterInputDialog;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;


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

    /**
     * Interpreter manager we are using (given at init)
     */
    private IInterpreterManager interpreterManager;
    
    /**
     * Tree to add libs.
     */
    private Tree treeWithLibs;

    /**
     * This is the control where the interpreters are shown
     */
    private Tree treeWithInterpreters;

    /**
     * Images
     */
    private Image imageSystemLibRoot;

    /**
     * Images
     */
    private Image imageSystemLib;

    private Composite box;

    private Button addBtForcedBuiltins;

    private Button removeBtForcedBuiltins;

    private SelectionListener selectionListenerOthers;
    
    private boolean changed;

    private List listBuiltins;

    private Composite boxSystem;

    private Button addBtSystemFolder;

    private Button removeBtSystemFolder;

    private Button addBtSystemJar;

    private SelectionListener selectionListenerSystem;
    
    private Map<String, IInterpreterInfo> nameToInfo = new HashMap<String, IInterpreterInfo>();

    public IInterpreterInfo[] getExesList(){
        TreeItem[] items = treeWithInterpreters.getItems();
        ArrayList<IInterpreterInfo> infos = new ArrayList<IInterpreterInfo>();
        for (TreeItem exe : items) {
            IInterpreterInfo info = this.nameToInfo.get(getNameFromTreeItem(exe));
            if(info == null){
                PydevPlugin.log("Didn't expect interpreter info to be null in the memory: "+exe);
            }else{
                infos.add(info);
            }
        }
        return infos.toArray(new IInterpreterInfo[infos.size()]);
    }

    protected String getNameFromTreeItem(TreeItem treeItem) {
        return treeItem.getText(0);
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
        
        IInterpreterInfo[] interpreters = this.interpreterManager.getInterpreterInfos();
        this.nameToInfo.clear();
        for (IInterpreterInfo interpreterInfo: interpreters) {
            if(interpreterInfo != null){
                nameToInfo.put(interpreterInfo.getName(), interpreterInfo.makeCopy());
            }
        }
        
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
        treeWithInterpreters = getListControl(parent);
        treeWithInterpreters.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                updateTree();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                updateTree();
            }

        });
        
        treeWithInterpreters.addKeyListener(new KeyListener(){
        
            public void keyReleased(KeyEvent e) {
            }
        
            public void keyPressed(KeyEvent e) {
                if(e.keyCode == SWT.F2){
                    renameSelection();
                }
            }
        });
        
        treeWithInterpreters.addMouseListener(new MouseListener(){
        
            public void mouseUp(MouseEvent e) {
            }
        
            public void mouseDown(MouseEvent e) {
            }
        
            public void mouseDoubleClick(MouseEvent e) {
                renameSelection();
            }
        });
    }
    
    
    private void renameSelection(){
        int index = getSelectionIndex();
        if(index >= 0){
            TreeItem curr = treeWithInterpreters.getItem(index);
            
            final String initialName = getNameFromTreeItem(curr);
            InputDialog d = new InputDialog(
                    this.getShell(), 
                    "New name", "Please specify the new name of the interpreter.", 
                    initialName, 
                    new IInputValidator(){
                        public String isValid(String newText) {
                            if(newText == null || newText.trim().equals("")){
                                return "Please specify a non-empty name.";
                            }
                            newText = newText.trim();
                            if(newText.equals(initialName)){
                                return null;
                            }
                            return getDuplicatedMessageError(newText, null);
                        }
                    });
            
            int retCode = d.open();
            if (retCode == InputDialog.OK) {
                String newName = d.getValue().trim();
                if(!newName.equals(initialName)){
                    IInterpreterInfo info = this.nameToInfo.get(initialName);
                    info.setName(newName);
                    curr.setText(0, newName);
                    this.nameToInfo.remove(initialName);
                    this.nameToInfo.put(newName, info);
                }
            }
        }
    }

    
    /**
     * @param parent
     * @return
     */
    private Tree getTreeLibsControl(Composite parent) {
        if (treeWithLibs == null){
            treeWithLibs = new Tree(parent, SWT.BORDER|SWT.MULTI);
            treeWithLibs.setFont(parent.getFont());
            treeWithLibs.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    treeWithLibs = null;
                }
            });
        }
        return treeWithLibs;
    }

    /**
     * Notifies that the Remove button has been pressed.
     */
    protected void removePressed() {
        super.removePressed();
        updateTree();
//        changed = true;
    }
    
    @Override
    protected void disposeOfTreeItem(TreeItem t) {
        String nameFromTreeItem = this.getNameFromTreeItem(t);
        this.nameToInfo.remove(nameFromTreeItem);
        super.disposeOfTreeItem(t);
    }

    protected void addPressed() {
        super.addPressed();
        updateTree();
//        changed = true;
    }

    protected void upPressed() {
        super.upPressed();
//        changed = true;
    }
    
    protected void downPressed() {
        super.downPressed();
//        changed = true;
    }
    
    protected void adjustForNumColumns(int numColumns) {
        super.adjustForNumColumns(numColumns);
        ((GridData) tabFolder.getLayoutData()).horizontalSpan = numColumns;
    }
    
    protected TabFolder tabFolder;

    private EnvironmentTab environmentTab;

    private MyEnvWorkingCopy workingCopy = new MyEnvWorkingCopy();
    
    /**
     * @see org.eclipse.jface.preference.ListEditor#doFillIntoGrid(org.eclipse.swt.widgets.Composite, int)
     */
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        GridData gd = new GridData();
        
        tabFolder = new TabFolder(parent, SWT.None);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalSpan  = numColumns;
        tabFolder.setLayoutData(gd);
        
        createTreeLibsControlTab();
        createForcedBuiltinsTab();
        createEnvironmentVariablesTab();
    }
    
    /**
     * Creates tab to show the environment variables.
     */
    private void createEnvironmentVariablesTab() {
        Composite parent;
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("Environment");
        
        
        Composite composite = new Composite(tabFolder, SWT.None);
        parent = composite;
        composite.setLayout(new GridLayout(1, false));
        
        environmentTab = new EnvironmentTab(){
            protected void createAppendReplace(Composite parent) {
                super.createAppendReplace(parent);
                appendEnvironment.setVisible(false);
                replaceEnvironment.setVisible(false);
            }
        };
        environmentTab.createControl(parent);
        
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        environmentTab.getControl().setLayoutData(gd);
        
        tabItem.setControl(composite);
    }

    /**
     * Creates tab to show the pythonpath (libraries)
     */
    private void createTreeLibsControlTab() {
        Composite parent;
        GridData gd;
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("Libraries");

        Composite composite = new Composite(tabFolder, SWT.None);
        parent = composite;
        composite.setLayout(new GridLayout(2, false));
        
        Label l1 = new Label(parent, SWT.None);
        l1.setText("System PYTHONPATH");
        gd = new GridData();
        gd.horizontalSpan = 2;
        l1.setLayoutData(gd);

        //the tree
        treeWithLibs = getTreeLibsControl(parent);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.heightHint = 200;
        treeWithLibs.setLayoutData(gd);

        //buttons at the side of the tree
        Composite control = getButtonBoxControlSystem(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        control.setLayoutData(gd);
        
        tabItem.setControl(composite);
    }

    /**
     * Creates tab for the forced builtins
     */
    private void createForcedBuiltinsTab() {
        Composite parent;
        GridData gd;
        TabItem tabItem;
        Composite composite;
        Composite control;
        tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("Forced Builtins");
        
        composite = new Composite(tabFolder, SWT.None);
        parent = composite;
        composite.setLayout(new GridLayout(2, false));

        
        //label
        Link l2 = new Link(parent, SWT.None);
        l2.setText("Forced Builtins (check <a>Manual</a> for more info).");
        l2.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                Program.launch("http://fabioz.com/pydev/manual_101_interpreter.html");
            }}
        );
        
        gd = new GridData();
        gd.horizontalSpan = 2;
        l2.setLayoutData(gd);

        //the list with the builtins
        List list = getBuiltinsListControl(parent);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.heightHint = 200;
        list.setLayoutData(gd);
        
        //the builtins buttons
        control = getButtonBoxControlOthers(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        control.setLayoutData(gd);
        tabItem.setControl(composite);
    }
    
    
    /**
     * @param parent
     * @return
     */
    private List getBuiltinsListControl(Composite parent) {
        if (listBuiltins == null) {
            listBuiltins = new List(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
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
                addBtSystemJar = createBt(boxSystem, "New Jar/Zip(s)", getSelectionListenerSystem());//$NON-NLS-1$
            }else{
                addBtSystemJar = createBt(boxSystem, "New Egg/Zip(s)", getSelectionListenerSystem());//$NON-NLS-1$
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
            addBtForcedBuiltins = createBt(box, "ListEditor.add", getSelectionListenerForcedBuiltins());//$NON-NLS-1$
            removeBtForcedBuiltins = createBt(box, "ListEditor.remove", getSelectionListenerForcedBuiltins());//$NON-NLS-1$
            box.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    addBtForcedBuiltins = null;
                    removeBtForcedBuiltins = null;
                    box = null;
                }
            });

        } else {
            checkParent(box, parent);
        }

        return box;
    }

    /**
     * Returns this field editor's selection listener. The listener is created if necessary.
     * 
     * @return the selection listener
     */
    private SelectionListener getSelectionListenerForcedBuiltins() {
        if (selectionListenerOthers == null){
            selectionListenerOthers = new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    Widget widget = event.widget;
                    if (widget == addBtForcedBuiltins) {
                        addForcedBuiltins();
                    } else if (widget == removeBtForcedBuiltins) {
                        removeForcedBuiltins();
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
                    if (treeWithInterpreters.getSelectionCount() == 1) {
                        TreeItem[] selection = treeWithInterpreters.getSelection();
                        InterpreterInfo info = (InterpreterInfo) nameToInfo.get(getNameFromTreeItem(selection[0]));

                    
                        Widget widget = event.widget;
                        if (widget == addBtSystemFolder) {
                            DirectoryDialog dialog = new DirectoryDialog(getShell());
                            dialog.setFilterPath(lastDirectoryDialogPath);
                            String filePath = dialog.open();
                            if(filePath != null){
                                lastDirectoryDialogPath = filePath;
                                info.libs.add(filePath);
//                                changed = true;
                            }
                            
                        } else if (widget == addBtSystemJar) {
                            FileDialog dialog = new FileDialog(getShell(), SWT.PRIMARY_MODAL|SWT.MULTI);
                            
                            if(AbstractInterpreterEditor.this.interpreterManager.isJython()){
                                dialog.setFilterExtensions(FileTypesPreferencesPage.getWildcardJythonValidZipFiles());
                            }else{
                                dialog.setFilterExtensions(FileTypesPreferencesPage.getWildcardPythonValidZipFiles());
                            }
                            
                            dialog.setFilterPath(lastFileDialogPath);
                            String filePath = dialog.open();
                            if(filePath != null){
                                lastFileDialogPath = filePath;
                                File filePath1 = new File(filePath);
                                String dir = filePath1.getParent();
                                
                                String[] fileNames = dialog.getFileNames();
                                for(String f:fileNames){
                                    f = dir+File.separatorChar+f;
                                    if(!info.libs.contains(f)){
                                        info.libs.add(f);
                                    }
                                }
//                                changed = true;
                            }
                                
                        } else if (widget == removeBtSystemFolder) {
                            TreeItem[] libSelection = treeWithLibs.getSelection();
                            for (int i = 0; i < libSelection.length; i++) {
                                TreeItem s = libSelection[i];
                                String text = s.getText();
                                info.libs.remove(text);
//                                changed = true;
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
    protected void addForcedBuiltins() {
        if (treeWithInterpreters.getSelectionCount() == 1) {
            TreeItem[] selection = treeWithInterpreters.getSelection();
            InterpreterInfo info = (InterpreterInfo) this.nameToInfo.get(getNameFromTreeItem(selection[0]));
            
            IInputValidator validator = new IInputValidator(){
            
                public String isValid(String newText) {
                    for(char c:newText.toCharArray()){
                        if(!Character.isJavaIdentifierPart(c) && c != ' ' && c != ',' && c != '.'){
                            return "Can only accept valid python module names (char: '"+c+"' not accepted)";
                        }
                    }
                    return null;
                }
            };;
            InputDialog d = new InputDialog(this.getShell(), "Builtin to add", "Builtin to add (comma separated)", "", validator);
            
            int retCode = d.open();
            if (retCode == InputDialog.OK) {
                String builtins = d.getValue();
                java.util.List<String> split = StringUtils.splitAndRemoveEmptyTrimmed(builtins, ',');
                for (String string : split) {
                    String trimmed = string.trim();
                    if(trimmed.length() > 0){
                        info.addForcedLib(trimmed);
                    }
                }
//                changed = true;
            }

        }
        updateTree();
    }

    /**
     * 
     */
    protected void removeForcedBuiltins() {
        if (treeWithInterpreters.getSelectionCount() == 1) {
            TreeItem[] interpreterSelection = treeWithInterpreters.getSelection();
            String[] builtins = listBuiltins.getSelection();
            
            InterpreterInfo info = (InterpreterInfo) this.nameToInfo.get(getNameFromTreeItem(interpreterSelection[0]));
            for(String builtin : builtins){
                info.removeForcedLib(builtin);
            }
//            changed = true;
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
        int index = this.getSelectionIndex();
        if (index >= 0) {
            TreeItem item = treeWithInterpreters.getItem(index);
            fillPathItemsFromName(getNameFromTreeItem(item));
        }else{
            fillPathItemsFromName(null);
            if (treeWithInterpreters.getItemCount() > 0){
                treeWithInterpreters.select(treeWithInterpreters.getItem(0));
                selectionChanged();
                fillPathItemsFromName(getNameFromTreeItem(treeWithInterpreters.getItem(0)));
            }
        }
    }


    /**
     * @param s
     * 
     */
    private void fillPathItemsFromName(String name) {
        treeWithLibs.removeAll();
        listBuiltins.removeAll();
        
        //before any change, apply the changes in the previous info (if not set, that's ok)
        if(workingCopy.getInfo() != null){
            environmentTab.performApply(workingCopy);
        }
        
        if(name != null){
            TreeItem item = new TreeItem(treeWithLibs, SWT.NONE);
            item.setText("System libs");
            item.setImage(imageSystemLibRoot);

            InterpreterInfo info = (InterpreterInfo) this.nameToInfo.get(name);
            if(info == null){
                PydevPlugin.log("Didn't expect interpreter info to be null in the memory: "+name);
            }else{
                for (Iterator<String> iter = info.libs.iterator(); iter.hasNext();) {
                    TreeItem subItem = new TreeItem(item, SWT.NONE);
                    subItem.setText(iter.next());
                    subItem.setImage(imageSystemLib);
                }
                item.setExpanded(true);
                
                //set the forced builtins
                for (Iterator<String> iter = info.forcedLibsIterator(); iter.hasNext();) {
                    listBuiltins.add((String) iter.next());
                }
            }
            
            workingCopy.setInfo(info);
            environmentTab.initializeFrom(workingCopy);
        }
        

    }


    /**
     * @return a string with the extensions that are accepted for the interpreter
     */
    public abstract String[] getInterpreterFilterExtensions();
    
    /** Overridden
     */
    protected Tuple<String, String> getNewInputObject(boolean autoConfig) {
        CharArrayWriter charWriter = new CharArrayWriter();
        PrintWriter logger = new PrintWriter(charWriter);
        logger.println("Information about process of adding new interpreter:");
        try {
            Tuple<String, String> interpreterNameAndExecutable = null;
            if(autoConfig){
                interpreterNameAndExecutable = getAutoNewInput();
                if(interpreterNameAndExecutable == null){
                    reportAutoConfigProblem(null);
                    return null;
                }
            }else{
                
                InterpreterInputDialog dialog = new InterpreterInputDialog(getShell(),
                        "Select interpreter",
                        "Enter the name and executable of your interpreter",this);
                
                logger.println("- Opening dialog to request executable (or jar).");
                int result = dialog.open();
                
                if (result == Window.OK){
                    interpreterNameAndExecutable = dialog.getKeyAndValueEntered();
                    if(interpreterNameAndExecutable == null){
                        ErrorDialog.openError(this.getShell(), "Error getting info on interpreter", 
                                "interpreterNameAndExecutable == null", 
                                PydevPlugin.makeStatus(IStatus.ERROR, "interpreterNameAndExecutable == null", new RuntimeException()));
                        return null;
                    }
                    String error = getDuplicatedMessageError(interpreterNameAndExecutable.o1, interpreterNameAndExecutable.o2);
                    if(error != null){
                        ErrorDialog.openError(this.getShell(), "Error getting info on interpreter", 
                                error, 
                                PydevPlugin.makeStatus(IStatus.ERROR, "Duplicated interpreter information", new RuntimeException()));
                        return null;
                    }
                }else{
                    return null;
                }
            }
            
            
            if (interpreterNameAndExecutable != null) {
                logger.println("- Chosen interpreter (name and file):'"+interpreterNameAndExecutable);
                if (interpreterNameAndExecutable.o2.trim().length() == 0){
                    logger.println("- When trimmed, the chosen file was empty (returning null).");
                    return null;
                }
            }else{
                logger.println("- The file chosen was null (returning null).");
                return null;
            }
            
            if (interpreterNameAndExecutable != null && interpreterNameAndExecutable.o2 != null) {
                //ok, now that we got the file, let's see if it is valid and get the library info.
                logger.println("- Ok, file is non-null. Getting info on:"+interpreterNameAndExecutable.o2);
                ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(this.getShell());
                monitorDialog.setBlockOnOpen(false);
                ObtainInterpreterInfoOperation operation = new ObtainInterpreterInfoOperation(interpreterNameAndExecutable.o2, logger, interpreterManager);
                monitorDialog.run(true, false, operation);

                if (operation.e != null) {
                    logger.println("- Some error happened while getting info on the interpreter:");
                    operation.e.printStackTrace(logger);

                    if(operation.e instanceof SimpleJythonRunner.JavaNotConfiguredException){
                        SimpleJythonRunner.JavaNotConfiguredException javaNotConfiguredException = (SimpleJythonRunner.JavaNotConfiguredException) operation.e;
                        
                        ErrorDialog.openError(this.getShell(), "Error getting info on interpreter", 
                                javaNotConfiguredException.getMessage(), 
                                PydevPlugin.makeStatus(IStatus.ERROR, "Java vm not configured.\n", javaNotConfiguredException));
                        
                    }else if(operation.e instanceof JDTNotAvailableException){
                        JDTNotAvailableException noJdtException = (JDTNotAvailableException) operation.e;
                        ErrorDialog.openError(this.getShell(), "Error getting info on interpreter", 
                                noJdtException.getMessage(), 
                                PydevPlugin.makeStatus(IStatus.ERROR, "JDT not available.\n", noJdtException));
                        
                    }else{
                        if(autoConfig){
                            reportAutoConfigProblem(operation.e);
                            
                        }else{
                            String errorMsg = "Some error happened while getting info on the interpreter.\n\n" +
                                        "Common reasons include:\n\n" +
                                        "- Specifying an invalid interpreter" +
                                        "(usually a link to the actual interpreter on Mac or Linux)\n" +
                                        "- Having spaces in your Eclipse installation path.";
                            //show the user a message (so that it does not fail silently)...
                            ErrorDialog.openError(this.getShell(), "Error getting info on interpreter", 
                                    errorMsg, 
                                    PydevPlugin.makeStatus(IStatus.ERROR, "Check your error log for more details.\n\n" +
                                        "More info can also be found at the bug report: http://sourceforge.net/tracker/index.php?func=detail&aid=1523582&group_id=85796&atid=577329", 
                                    operation.e));
                        }
                    }
                    
                    throw operation.e;
                }

                if(operation.result != null){
                    operation.result.setName(interpreterNameAndExecutable.o1);
                    logger.println("- Success getting the info. Result:"+operation.result);
                    
                    this.nameToInfo.put(operation.result.getName(), operation.result.makeCopy());
    
                    return new Tuple<String, String>(operation.result.getName(), operation.result.executableOrJar);
                }else{
                    return null;
                }
            }
            
        } catch (Exception e) {
            PydevPlugin.log(e);
            return null;
        } finally {
            PydevPlugin.logInfo(charWriter.toString());
        }
        
        return null;
    }

    /**
     * Gets a unique name for the interpreter based on an initial expected name.
     */
    public String getUniqueInterpreterName(final String expectedName) {
        String additional = "";
        int i = 0;
        while(getDuplicatedMessageError(expectedName+additional, null) != null){
            i++;
            additional = String.valueOf(i);
        }
        return expectedName+additional;
    }
    
    /**
     * Uses the passed name and executable to see if it'll match against one of the existing 
     * 
     * The null parameters are ignored.
     */
    public String getDuplicatedMessageError(String interpreterName, String executableOrJar) {
        String error = null;
        if(interpreterName != null){
            interpreterName = interpreterName.trim();
            if(this.nameToInfo.containsKey(interpreterName)){
                error = "An interpreter is already configured with the name: "+interpreterName;
            }
        }
        if(executableOrJar != null){
            executableOrJar = executableOrJar.trim();
            for(IInterpreterInfo info:this.nameToInfo.values()){
                if(info.getExecutableOrJar().trim().equals(executableOrJar)){
                    error = "An interpreter is already configured with the path: "+executableOrJar;
                }
            }
        }
        return error;
    }

    private void reportAutoConfigProblem(Exception e) {
        String errorMsg = 
            "Unable to auto-configure the interpreter.\n" +
        	"Please create a new interpreter using the 'New' button.";
        ErrorDialog.openError(this.getShell(), "Unable to auto-configure.", 
                errorMsg, 
                PydevPlugin.makeStatus(IStatus.ERROR, "Unable to gather the needed info from the system.", 
                        e));
    }

    /**
     * @return a tuple with the name of the interpreter and the string with the file to be executed 
     * (for python could be just python.exe) and for jython the jython.jar location.
     * 
     * This is also be platform-dependent (so, it could be python.exe or just python)
     * 
     * If it cannot be determined, the return should be null (and not a tuple with empty values)
     */
    protected abstract Tuple<String, String> getAutoNewInput();
    

    @Override
    protected void doStore() {
        //The doStore is called before hasChanged, so, at this point, we set the changed variable and update the persisted
        //string (if needed)
        
        //we need to update the tree so that the environment variables stay correct. 
        this.updateTree();
        
        String newStringToPersist = createListFromInterpreterInfo(getExesList());
        String oldStringToPersist = createListFromInterpreterInfo(interpreterManager.getInterpreterInfos());
        if(!newStringToPersist.equals(oldStringToPersist)){
            interpreterManager.setPersistedString(newStringToPersist);
            changed = true;
        }else{
            changed = false;
        }

    }
    
    @Override
    protected void doLoad() {
        if (treeWithInterpreters != null) {
            String s = interpreterManager.getPersistedString();
            IInterpreterInfo[] array = parseStringToInfo(s);
            this.nameToInfo.clear();
            for (int i = 0; i < array.length; i++) {
                IInterpreterInfo interpreterInfo = array[i];
                createInterpreterItem(interpreterInfo.getName(), interpreterInfo.getExecutableOrJar());
                this.nameToInfo.put(interpreterInfo.getName(), interpreterInfo.makeCopy());
            }
        }
        updateTree();
    }

    public String getPreferenceName(){
        throw new RuntimeException("The preferences should be stored/gotten from the IInterpreterManager, and not directly.");
    }
    
    
    /** Overridden
     */
    protected String createListFromInterpreterInfo(IInterpreterInfo[] executables) {
        return interpreterManager.getStringToPersist(executables);
    }
    
    
    /** Overridden
     */
    protected IInterpreterInfo[] parseStringToInfo(String stringList) {
        return interpreterManager.getInterpretersFromPersistedString(stringList);
    }

    
    
    /**
     * @see org.python.copiedfromeclipsesrc.PythonListEditor#doLoadDefault()
     */
    protected void doLoadDefault() {
        //do nothing
    }

    
    public boolean checkChangedAndMarkUnchanged() {
        //doStore was called before and should've updated the changed state properly.
        boolean ret = changed;
        changed = false;
        return ret;
    }

}
