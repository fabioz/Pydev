/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Sep 8, 2003
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.python.copiedfromeclipsesrc.PythonListEditor;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IInterpreterManagerListener;
import org.python.pydev.core.PropertiesHelper;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.tree.EnabledTreeDragReorder;
import org.python.pydev.ui.TabVariables;
import org.python.pydev.ui.dialogs.InterpreterInputDialog;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
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

public abstract class AbstractInterpreterEditor extends PythonListEditor implements IInterpreterManagerListener {

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
    /*default*/Tree treeWithInterpreters;

    /**
     * Images
     */
    private Image imageSystemLibRoot;
    private Image imageSystemLib;
    private Image environmentImage;

    private Composite boxSystem;

    private Button addBtSystemFolder;

    private Button removeBtSystemFolder;

    private Button addBtSystemJar;

    private SelectionListener selectionListenerSystem;

    private Map<String, IInterpreterInfo> nameToInfo = new HashMap<String, IInterpreterInfo>();

    public Map<String, IInterpreterInfo> getNameToInfo() {
        return nameToInfo;
    }

    private Set<String> exeOrJarOfInterpretersToRestore = new HashSet<String>();
    private Set<String> exeOrJarOfInterpretersWithBuiltinsChanged = new HashSet<String>();
    private Set<String> exeOrJarOfInterpretersWithPredefinedChanged = new HashSet<String>();
    private Set<String> exeOrJarOfInterpretersWithStringSubstitutionChanged = new HashSet<String>();

    private void clearInfos() {
        nameToInfo.clear();
        exeOrJarOfInterpretersToRestore.clear();
        exeOrJarOfInterpretersWithBuiltinsChanged.clear();
        exeOrJarOfInterpretersWithPredefinedChanged.clear();
        exeOrJarOfInterpretersWithStringSubstitutionChanged.clear();
    }

    public Set<String> getInterpreterExeOrJarToRestoreAndClear() {
        HashSet<String> set = new HashSet<String>();
        set.addAll(exeOrJarOfInterpretersToRestore);
        set.addAll(exeOrJarOfInterpretersWithBuiltinsChanged);
        set.addAll(exeOrJarOfInterpretersWithPredefinedChanged);
        set.addAll(exeOrJarOfInterpretersWithStringSubstitutionChanged);

        exeOrJarOfInterpretersToRestore.clear();
        exeOrJarOfInterpretersWithBuiltinsChanged.clear();
        exeOrJarOfInterpretersWithPredefinedChanged.clear();
        exeOrJarOfInterpretersWithStringSubstitutionChanged.clear();

        return set;
    }

    public IInterpreterInfo[] getExesList() {
        TreeItem[] items = treeWithInterpreters.getItems();
        ArrayList<IInterpreterInfo> infos = new ArrayList<IInterpreterInfo>();
        for (TreeItem exe : items) {
            IInterpreterInfo info = this.nameToInfo.get(getNameFromTreeItem(exe));
            if (info == null) {
                Log.log("Didn't expect interpreter info to be null in the memory: " + exe);
            } else {
                infos.add(info);
            }
        }
        return infos.toArray(new IInterpreterInfo[infos.size()]);
    }

    protected String getNameFromTreeItem(TreeItem treeItem) {
        return treeItem.getText(0);
    }

    /*default*/InterpreterInfo getSelectedInfo() {
        if (treeWithInterpreters.getSelectionCount() == 1) {
            TreeItem[] selection = treeWithInterpreters.getSelection();
            return (InterpreterInfo) this.nameToInfo.get(getNameFromTreeItem(selection[0]));
        }
        return null;
    }

    /**
     * Creates a path field editor linked to the preference name passed
     *
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    protected AbstractInterpreterEditor(String preferenceName, String labelText, Composite parent,
            IInterpreterManager interpreterManager) {
        init(preferenceName, labelText);
        this.interpreterManager = interpreterManager;

        IInterpreterInfo[] interpreters = this.interpreterManager.getInterpreterInfos();
        this.interpreterManager.addListener(this);
        clearInfos();
        for (IInterpreterInfo interpreterInfo : interpreters) {
            if (interpreterInfo != null) {
                nameToInfo.put(interpreterInfo.getName(), interpreterInfo.makeCopy());
            }
        }

        if (USE_ICONS) {
            ImageCache imageCache = PydevPlugin.getImageCache();
            imageSystemLibRoot = imageCache.get(UIConstants.LIB_SYSTEM_ROOT);
            imageSystemLib = imageCache.get(UIConstants.LIB_SYSTEM);
            environmentImage = imageCache.get(UIConstants.ENVIRONMENT_ICON);
        }
        createControl(parent);
        updateTree();
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditor#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
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

        treeWithInterpreters.addKeyListener(new KeyListener() {

            public void keyReleased(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.F2) {
                    renameSelection();
                }
            }
        });

        treeWithInterpreters.addMouseListener(new MouseListener() {

            public void mouseUp(MouseEvent e) {
            }

            public void mouseDown(MouseEvent e) {
            }

            public void mouseDoubleClick(MouseEvent e) {
                renameSelection();
            }
        });
    }

    private void renameSelection() {
        int index = getSelectionIndex();
        if (index >= 0) {
            TreeItem curr = treeWithInterpreters.getItem(index);

            final String initialName = getNameFromTreeItem(curr);
            InputDialog d = new InputDialog(this.getShell(), "New name",
                    "Please specify the new name of the interpreter.", initialName, new IInputValidator() {
                        public String isValid(String newText) {
                            if (newText == null || newText.trim().equals("")) {
                                return "Please specify a non-empty name.";
                            }
                            newText = newText.trim();
                            if (newText.equals(initialName)) {
                                return null;
                            }
                            return InterpreterConfigHelpers.getDuplicatedMessageError(
                                    newText, null, nameToInfo);
                        }
                    });

            int retCode = d.open();
            if (retCode == InputDialog.OK) {
                String newName = d.getValue().trim();
                if (!newName.equals(initialName)) {
                    IInterpreterInfo info = this.nameToInfo.get(initialName);
                    info.setName(newName);
                    curr.setText(0, newName);
                    this.nameToInfo.remove(initialName);
                    this.nameToInfo.put(newName, info);
                    this.exeOrJarOfInterpretersToRestore.add(info.getExecutableOrJar());
                }
            }
        }
    }

    /**
     * @param parent
     * @return
     */
    private Tree getTreeLibsControl(Composite parent) {
        if (treeWithLibs == null) {
            treeWithLibs = new Tree(parent, SWT.BORDER | SWT.MULTI);
            treeWithLibs.setFont(parent.getFont());
            treeWithLibs.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    treeWithLibs = null;
                }
            });
            EnabledTreeDragReorder.enableDrag(treeWithLibs, false, new ICallback<Object, Object>() {

                @Override
                public Object call(Object arg) {
                    if (treeWithInterpreters.getSelectionCount() == 1) {
                        TreeItem[] selection = treeWithInterpreters.getSelection();
                        String nameFromTreeItem = getNameFromTreeItem(selection[0]);
                        InterpreterInfo info = (InterpreterInfo) nameToInfo.get(nameFromTreeItem);
                        exeOrJarOfInterpretersToRestore.add(info.getExecutableOrJar());

                        info.libs.clear();
                        TreeItem[] items = treeWithLibs.getItems();
                        if (items.length == 1) {
                            TreeItem treeItem = items[0];
                            TreeItem[] items2 = treeItem.getItems();
                            for (TreeItem treeItem2 : items2) {
                                info.libs.add(treeItem2.getText(0));
                            }
                        }
                    }
                    return null;
                }
            });
        }
        return treeWithLibs;
    }

    @Override
    protected void disposeOfTreeItem(TreeItem t) {
        String nameFromTreeItem = this.getNameFromTreeItem(t);
        this.nameToInfo.remove(nameFromTreeItem);
        super.disposeOfTreeItem(t);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        super.adjustForNumColumns(numColumns);
        ((GridData) tabFolder.getLayoutData()).horizontalSpan = numColumns;
    }

    protected TabFolder tabFolder;

    private EnvironmentTab environmentTab;

    private MyEnvWorkingCopy workingCopy = new MyEnvWorkingCopy();

    private TabVariables tabVariables;

    private AbstractListWithNewRemoveControl forcedBuiltins;

    private AbstractListWithNewRemoveControl predefinedCompletions;

    /**
     * @see org.eclipse.jface.preference.ListEditor#doFillIntoGrid(org.eclipse.swt.widgets.Composite, int)
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        GridData gd = new GridData();

        tabFolder = new TabFolder(parent, SWT.None);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalSpan = numColumns;
        tabFolder.setLayoutData(gd);

        createTreeLibsControlTab();

        //----------------------- FORCED BUILTINS
        forcedBuiltins = new AbstractListWithNewRemoveControl(this) {

            @Override
            protected List<String> getStringsFromInfo(InterpreterInfo info) {
                ArrayList<String> ret = new ArrayList<String>();
                for (Iterator<String> iter = info.forcedLibsIterator(); iter.hasNext();) {
                    ret.add(iter.next());
                }
                return ret;
            }

            @Override
            protected void removeSelectedFrominfo(InterpreterInfo info, String[] builtins) {
                for (String builtin : builtins) {
                    info.removeForcedLib(builtin);
                }
                exeOrJarOfInterpretersWithBuiltinsChanged.add(info.getExecutableOrJar());
            }

            @Override
            protected String getInput() {
                IInputValidator validator = new IInputValidator() {

                    public String isValid(String newText) {
                        for (char c : newText.toCharArray()) {
                            if (!Character.isJavaIdentifierPart(c) && c != ' ' && c != ',' && c != '.') {
                                return "Can only accept valid python module names (char: '" + c + "' not accepted)";
                            }
                        }
                        return null;
                    }
                };
                InputDialog d = new InputDialog(getShell(), "Builtin to add", "Builtin to add (comma separated)", "",
                        validator);

                int retCode = d.open();
                String builtins = null;
                if (retCode == InputDialog.OK) {
                    builtins = d.getValue();
                }
                return builtins;
            }

            @Override
            protected void addInputToInfo(InterpreterInfo info, String builtins) {
                java.util.List<String> split = StringUtils.splitAndRemoveEmptyTrimmed(builtins, ',');
                for (String string : split) {
                    String trimmed = string.trim();
                    if (trimmed.length() > 0) {
                        info.addForcedLib(trimmed);
                    }
                }
                exeOrJarOfInterpretersWithBuiltinsChanged.add(info.getExecutableOrJar());
            }

        };
        forcedBuiltins.createTab("Forced Builtins", "Forced Builtins (check <a>Manual</a> for more info).");

        //----------------------- PREDEFINED COMPLETIONS
        predefinedCompletions = new AbstractListWithNewRemoveControl(this) {

            private Button addAPIBt;

            @Override
            protected List<String> getStringsFromInfo(InterpreterInfo info) {
                return info.getPredefinedCompletionsPath();
            }

            @Override
            protected void removeSelectedFrominfo(InterpreterInfo info, String[] items) {
                for (String item : items) {
                    info.removePredefinedCompletionPath(item);
                }
                exeOrJarOfInterpretersWithPredefinedChanged.add(info.getExecutableOrJar());
            }

            @Override
            protected String getInput() {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setFilterPath(lastDirectoryDialogPath);
                String filePath = dialog.open();
                if (filePath != null) {
                    lastDirectoryDialogPath = filePath;
                }
                return filePath;
            }

            @Override
            protected void addInputToInfo(InterpreterInfo info, String item) {
                info.addPredefinedCompletionsPath(item);
                exeOrJarOfInterpretersWithPredefinedChanged.add(info.getExecutableOrJar());
            }

            @Override
            protected void createButtons(AbstractInterpreterEditor interpreterEditor) {
                super.createButtons(interpreterEditor);
                addAPIBt = interpreterEditor.createBt(box, "Add from QScintilla api file", this);//$NON-NLS-1$
            }

            @Override
            public void widgetDisposed(DisposeEvent event) {
                super.widgetDisposed(event);
                if (addAPIBt != null) {
                    addAPIBt.dispose();
                    addAPIBt = null;
                }
            }

            @Override
            public void widgetSelected(SelectionEvent event) {
                super.widgetSelected(event);
                Widget widget = event.widget;
                if (widget == addAPIBt) {
                    addAPIBt();
                }
            }

            private void addAPIBt() {
                final AbstractInterpreterEditor interpreterEditor = this.container.get();
                Assert.isNotNull(interpreterEditor);

                final InterpreterInfo info = interpreterEditor.getSelectedInfo();
                if (info != null) {
                    FileDialog dialog = new FileDialog(getShell(), SWT.PRIMARY_MODAL | SWT.MULTI);

                    dialog.setFilterExtensions(new String[] { "*.api" });
                    dialog.setText("Select .api file to be converted to .pypredef.");

                    dialog.setFilterPath(lastFileDialogPath);
                    final String filePath = dialog.open();
                    if (filePath != null) {
                        lastFileDialogPath = filePath;
                        File filePath1 = new File(filePath);
                        final String dir = filePath1.getParent();

                        IInputValidator validator = new IInputValidator() {

                            public String isValid(String newText) {
                                if (newText.length() == 0) {
                                    return "Number not provided.";
                                }
                                try {
                                    Integer.parseInt(newText);
                                } catch (NumberFormatException e) {
                                    return "The string: " + newText + " is not a valid integer.";
                                }
                                return null;
                            }
                        };
                        final InputDialog d = new InputDialog(
                                getShell(),
                                "Number of tokens to consider module",
                                "Please specify the number of tokens to consider a module from the .api file\n\n"
                                        + "i.e.: if there's a PyQt4.QtCore.QObject and PyQt4.QtCore is a module and QtObject "
                                        + "is the first class, the number of tokens to consider a module would be 2 (one for "
                                        + "PyQt4 and another for QtCore).", "", validator);

                        int retCode = d.open();
                        final ByteArrayOutputStream output = new ByteArrayOutputStream();
                        if (retCode == InputDialog.OK) {

                            ProgressMonitorDialog monitorDialog = new AsynchronousProgressMonitorDialog(getShell());
                            monitorDialog.setBlockOnOpen(false);
                            final Exception[] exception = new Exception[1];
                            try {
                                IRunnableWithProgress operation = new IRunnableWithProgress() {

                                    public void run(final IProgressMonitor monitor) throws InvocationTargetException,
                                            InterruptedException {
                                        monitor.beginTask("Restoring PYTHONPATH", IProgressMonitor.UNKNOWN);

                                        IPythonInterpreter interpreter = JythonPlugin
                                                .newPythonInterpreter(false, false);
                                        interpreter.setErr(output);
                                        interpreter.setOut(output);
                                        HashMap<String, Object> locals = new HashMap<String, Object>();
                                        locals.put("api_file", filePath);
                                        locals.put("parts_for_module", d.getValue());
                                        locals.put("cancel_monitor", monitor);
                                        try {
                                            JythonPlugin.exec(locals, "convert_api_to_pypredef.py", interpreter);
                                        } catch (Exception e) {
                                            Log.log(e + "\n\n" + output.toString());
                                            exception[0] = e;
                                        }

                                        monitor.done();
                                    }
                                };

                                monitorDialog.run(true, true, operation);

                            } catch (Exception e) {
                                Log.log(e);
                            }

                            Exception e = exception[0];
                            String contents = output.toString();
                            if (e == null && contents.indexOf("SUCCESS") != -1) {
                                addInputToInfo(info, dir);
                                interpreterEditor.updateTree();
                            } else {
                                if (e != null) {
                                    MessageDialog.openError(getShell(), "Error creating .pypredef files",
                                            e.getMessage() + "\n\n" + contents);
                                } else {
                                    MessageDialog.openError(getShell(), "Error creating .pypredef files", contents);
                                }
                            }
                        }
                    }
                }
            }

        };
        predefinedCompletions.createTab("Predefined", "Predefined completions (check <a>Manual</a> for more info).");
        createEnvironmentVariablesTab();
        createStringSubstitutionTab();

    }

    /**
     * Creates tab to show the string substitution variables.
     */
    private void createStringSubstitutionTab() {
        Map<String, String> initialVariables = new HashMap<String, String>();
        tabVariables = new TabVariables(tabFolder, initialVariables);
    }

    /**
     * Creates tab to show the environment variables.
     */
    private void createEnvironmentVariablesTab() {
        Composite parent;
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("Environment");
        tabItem.setImage(environmentImage);

        Composite composite = new Composite(tabFolder, SWT.None);
        parent = composite;
        composite.setLayout(new GridLayout(1, false));

        environmentTab = new EnvironmentTab() {
            @Override
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
        tabItem.setImage(imageSystemLibRoot);

        Composite composite = new Composite(tabFolder, SWT.None);
        parent = composite;
        composite.setLayout(new GridLayout(2, false));

        Label l1 = new Label(parent, SWT.None);
        l1.setText("System PYTHONPATH.   Reorder with Drag && Drop.");
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
            switch (this.interpreterManager.getInterpreterType()) {

                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                    addBtSystemJar = createBt(boxSystem, "New Jar/Zip(s)", getSelectionListenerSystem());//$NON-NLS-1$
                    break;

                default:
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

    private static String lastDirectoryDialogPath = null;
    private static String lastFileDialogPath = null;

    /**
     * Returns this field editor's selection listener. The listener is created if necessary.
     *
     * @return the selection listener
     */
    private SelectionListener getSelectionListenerSystem() {
        if (selectionListenerSystem == null) {
            selectionListenerSystem = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    if (treeWithInterpreters.getSelectionCount() == 1) {
                        TreeItem[] selection = treeWithInterpreters.getSelection();
                        String nameFromTreeItem = getNameFromTreeItem(selection[0]);
                        InterpreterInfo info = (InterpreterInfo) nameToInfo.get(nameFromTreeItem);
                        exeOrJarOfInterpretersToRestore.add(info.getExecutableOrJar());

                        Widget widget = event.widget;
                        if (widget == addBtSystemFolder) {
                            DirectoryDialog dialog = new DirectoryDialog(getShell());
                            dialog.setFilterPath(lastDirectoryDialogPath);
                            String filePath = dialog.open();
                            if (filePath != null) {
                                lastDirectoryDialogPath = filePath;
                                if (!info.libs.contains(filePath)) {
                                    info.libs.add(filePath);
                                }
                            }

                        } else if (widget == addBtSystemJar) {
                            FileDialog dialog = new FileDialog(getShell(), SWT.PRIMARY_MODAL | SWT.MULTI);

                            switch (AbstractInterpreterEditor.this.interpreterManager.getInterpreterType()) {

                                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                                    dialog.setFilterExtensions(FileTypesPreferencesPage
                                            .getWildcardJythonValidZipFiles());
                                    break;

                                default:
                                    dialog.setFilterExtensions(FileTypesPreferencesPage
                                            .getWildcardPythonValidZipFiles());
                            }

                            dialog.setFilterPath(lastFileDialogPath);
                            String filePath = dialog.open();
                            if (filePath != null) {
                                lastFileDialogPath = filePath;
                                File filePath1 = new File(filePath);
                                String dir = filePath1.getParent();

                                String[] fileNames = dialog.getFileNames();
                                for (String f : fileNames) {
                                    f = dir + File.separatorChar + f;
                                    if (!info.libs.contains(f)) {
                                        info.libs.add(f);
                                    }
                                }
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
     * Helper method to create a push button.
     *
     * @param parent the parent control
     * @param key the resource name used to supply the button's label text
     * @param listenerToAdd
     * @return Button
     */
    /*default*/Button createBt(Composite parent, String key, SelectionListener listenerToAdd) {
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
    @Override
    public void updateTree() {
        int index = this.getSelectionIndex();
        if (index >= 0) {
            TreeItem item = treeWithInterpreters.getItem(index);
            fillPathItemsFromName(getNameFromTreeItem(item));
        } else {
            fillPathItemsFromName(null);
            if (treeWithInterpreters.getItemCount() > 0) {
                treeWithInterpreters.setSelection(treeWithInterpreters.getItem(0));
                selectionChanged();
                fillPathItemsFromName(getNameFromTreeItem(treeWithInterpreters.getItem(0)));
            }
        }
    }

    @Override
    public Shell getShell() {
        return super.getShell();
    }

    /**
     * @param s
     *
     */
    private void fillPathItemsFromName(String name) {
        treeWithLibs.removeAll();
        this.forcedBuiltins.removeAllFromList();
        this.predefinedCompletions.removeAllFromList();

        //before any change, apply the changes in the previous info (if not set, that's ok)
        InterpreterInfo workingCopyInfo = workingCopy.getInfo();
        if (workingCopyInfo != null) {
            environmentTab.performApply(workingCopy);
            Properties propertiesFromMap = PropertiesHelper.createPropertiesFromMap(this.tabVariables
                    .getTreeItemsAsMap());
            Properties stringSubstitutionVariables = workingCopyInfo.getStringSubstitutionVariables();
            boolean equals = false;
            if (stringSubstitutionVariables == null) {
                if (propertiesFromMap == null || propertiesFromMap.size() == 0) {
                    equals = true;
                }
            } else {
                equals = stringSubstitutionVariables.equals(propertiesFromMap);
            }
            if (!equals) {
                exeOrJarOfInterpretersWithStringSubstitutionChanged.add(workingCopyInfo.getExecutableOrJar());
                workingCopyInfo.setStringSubstitutionVariables(propertiesFromMap);
            }
        }

        if (name != null) {
            TreeItem item = new TreeItem(treeWithLibs, SWT.NONE);
            item.setText("System libs");
            item.setImage(imageSystemLibRoot);

            InterpreterInfo info = (InterpreterInfo) this.nameToInfo.get(name);
            if (info == null) {
                Log.log("Didn't expect interpreter info to be null in the memory: " + name);
            } else {
                for (Iterator<String> iter = info.libs.iterator(); iter.hasNext();) {
                    TreeItem subItem = new TreeItem(item, SWT.NONE);
                    subItem.setText(iter.next());
                    subItem.setImage(imageSystemLib);
                    subItem.setData(EnabledTreeDragReorder.DRAG_IMAGE_DATA_KEY, UIConstants.LIB_SYSTEM);
                }
                item.setExpanded(true);

                this.forcedBuiltins.update(info);
                this.predefinedCompletions.update(info);
                workingCopy.setInfo(info);
            }

            environmentTab.initializeFrom(workingCopy);
            Properties stringSubstitutionVariables = info.getStringSubstitutionVariables();
            if (stringSubstitutionVariables != null) {
                this.tabVariables.setTreeItemsFromMap(PropertiesHelper
                        .createMapFromProperties(stringSubstitutionVariables));
            } else {
                this.tabVariables.setTreeItemsFromMap(new HashMap<String, String>());
            }
        }

    }

    /**
     * @return a string with the extensions that are accepted for the interpreter
     */
    public abstract String[] getInterpreterFilterExtensions();

    /**
     * @return the type of the interpreter
     */
    public abstract IInterpreterProviderFactory.InterpreterType getInterpreterType();

    @Override
    protected Tuple<String, String> getNewInputObject(int configType) {
        CharArrayWriter charWriter = new CharArrayWriter();
        PrintWriter logger = new PrintWriter(charWriter);
        try {
            ObtainInterpreterInfoOperation operation = null;
            if (configType != InterpreterConfigHelpers.CONFIG_MANUAL) {
                //Auto-config
                AutoConfigMaker a = new AutoConfigMaker(getInterpreterType(),
                        configType == InterpreterConfigHelpers.CONFIG_ADV_AUTO, logger,
                        nameToInfo);
                operation = a.autoConfigSearch();
            } else {
                //Manual config
                logger.println("Information about process of adding new interpreter:");
                Tuple<String, String> interpreterNameAndExecutable = newConfig(logger);
                if (interpreterNameAndExecutable == null) {
                    return null;
                }
                interpreterNameAndExecutable.o1 = InterpreterConfigHelpers.getUniqueInterpreterName(
                        interpreterNameAndExecutable.o1, nameToInfo);
                boolean foundError = InterpreterConfigHelpers.checkInterpreterNameAndExecutable(
                        interpreterNameAndExecutable, logger, "Error getting info on interpreter",
                        nameToInfo, this.getShell());

                if (foundError) {
                    return null;
                }

                logger.println("- Chosen interpreter (name and file):'" + interpreterNameAndExecutable);

                if (interpreterNameAndExecutable != null && interpreterNameAndExecutable.o2 != null) {
                    //ok, now that we got the file, let's see if it is valid and get the library info.
                    operation = InterpreterConfigHelpers.tryInterpreter(
                            interpreterNameAndExecutable, interpreterManager,
                            false, true, logger, this.getShell());
                }
            }

            if (operation != null) {
                String newName = operation.result.getName();
                this.nameToInfo.put(newName, operation.result.makeCopy());
                exeOrJarOfInterpretersToRestore.add(operation.result.executableOrJar);

                return new Tuple<String, String>(operation.result.getName(),
                        operation.result.executableOrJar);
            }

        } catch (Exception e) {
            Log.log(e);
            return null;
        } finally {
            String logInfo = charWriter.toString();
            if (logInfo.length() > 0) {
                Log.logInfo(charWriter.toString());
            }
        }

        return null;
    }

    private Tuple<String, String> newConfig(PrintWriter logger) {
        InterpreterInputDialog dialog = new InterpreterInputDialog(getShell(), "Select interpreter",
                "Enter the name and executable of your interpreter", this);

        logger.println("- Opening dialog to request executable (or jar).");
        int result = dialog.open();

        if (result == Window.OK) {
            return dialog.getKeyAndValueEntered();
        }
        return null;

    }

    public static final class CancelException extends Exception {

        private static final long serialVersionUID = 1L;

    }

    public final CancelException cancelException = new CancelException();

    @Override
    protected void doStore() {
        //Do nothing (all handled in the preferences page regarding the store (no longer in this editor)
    }

    @Override
    protected void doLoad() {
        if (treeWithInterpreters != null) {
            treeWithInterpreters.removeAll();
            //Work with a copy of the interpreters actually configured.
            String s = interpreterManager.getPersistedString();
            IInterpreterInfo[] array = interpreterManager.getInterpretersFromPersistedString(s);
            clearInfos();
            for (int i = 0; i < array.length; i++) {
                IInterpreterInfo interpreterInfo = array[i];
                createInterpreterItem(interpreterInfo.getName(), interpreterInfo.getExecutableOrJar());
                this.nameToInfo.put(interpreterInfo.getName(), interpreterInfo.makeCopy());
            }
        }
        updateTree();
    }

    /**
     * Called after infos are set (changed) in the interpreter manager.
     */
    public void afterSetInfos(IInterpreterManager manager, IInterpreterInfo[] interpreterInfos) {
        synchronized (expectedSetLock) {
            if (expectedSetInfos == 0) {
                RunInUiThread.async(new Runnable() {

                    @Override
                    public void run() {
                        if (treeWithInterpreters != null && !treeWithInterpreters.isDisposed()) {
                            //If the UI is not current (i.e.: tree is not visible), don't bother asking...
                            if (!treeWithInterpreters.isVisible()
                                    || PyDialogHelpers
                                            .openQuestion("Info changed",
                                                    "Information on interpreters changed. Update UI?\nNote: if there's any change done in the UI it'll be lost.")) {
                                doLoad();
                            }
                        }
                    }
                }, true);
            }
        }
    }

    @Override
    public String getPreferenceName() {
        throw new RuntimeException(
                "The preferences should be stored/gotten from the IInterpreterManager, and not directly.");
    }

    /**
     * @see org.python.copiedfromeclipsesrc.PythonListEditor#doLoadDefault()
     */
    @Override
    protected void doLoadDefault() {
        doLoad();
    }

    private int expectedSetInfos = 0;
    private final Object expectedSetLock = new Object();

    public void pushExpectedSetInfos() {
        synchronized (expectedSetLock) {
            this.expectedSetInfos -= 1;
        }
    }

    public void popExpectedSetInfos() {
        synchronized (expectedSetLock) {
            this.expectedSetInfos += 1;
        }
    }

}
