/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.ast.codecompletion.PyCodeCompletionImages;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.debug.ui.launching.FileOrResource;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.pyunit.preferences.PyUnitPrefsPage2;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.dialogs.DialogMemento;
import org.python.pydev.ui.dialogs.TreeSelectionDialog;

class ShiftListener implements Listener {

    public boolean shiftPressed = false;
    public CallbackWithListeners<Boolean> onChanged = new CallbackWithListeners<>();

    public ShiftListener() {
    }

    @Override
    public void handleEvent(Event event) {
        if (event.keyCode == SWT.SHIFT) {
            if (event.type == SWT.KeyDown) {
                shiftPressed = true;
                onChanged.call(shiftPressed);
            } else if (event.type == SWT.KeyUp) {
                shiftPressed = false;
                onChanged.call(shiftPressed);
            }
        }
    }

}

public class RunEditorAsCustomUnitTestAction extends AbstractRunEditorAction {

    class UnittestLaunchShortcut extends AbstractLaunchShortcut {
        private final Tuple<String, IInterpreterManager> launchConfigurationTypeAndInterpreterManager;
        private final String arguments;

        UnittestLaunchShortcut(Tuple<String, IInterpreterManager> launchConfigurationTypeAndInterpreterManager,
                String arguments) {
            this.launchConfigurationTypeAndInterpreterManager = launchConfigurationTypeAndInterpreterManager;
            this.arguments = arguments;
        }

        @Override
        protected String getLaunchConfigurationType() {
            return launchConfigurationTypeAndInterpreterManager.o1;
        }

        @Override
        protected IInterpreterManager getInterpreterManager(IProject project) {
            return launchConfigurationTypeAndInterpreterManager.o2;
        }

        @Override
        public ILaunchConfigurationWorkingCopy createDefaultLaunchConfigurationWithoutSaving(
                FileOrResource[] resource) throws CoreException {
            ILaunchConfigurationWorkingCopy workingCopy = super.createDefaultLaunchConfigurationWithoutSaving(
                    resource);
            if (arguments.trim().length() > 0 && workingCopy != null) {
                // first remember the arguments to be used internally and for matching
                workingCopy.setAttribute(Constants.ATTR_UNITTEST_TESTS, arguments);
                // then determine the tests to be displayed to user
                String[] argumentsSplit = arguments.split(",");
                FastStringBuffer argsWithTests = new FastStringBuffer(workingCopy.getName(),
                        arguments.length() + 10);
                argsWithTests.append(" ( ");
                for (int i = 0; i < argumentsSplit.length; i++) {
                    if (i != 0) {
                        argsWithTests.append(", ");
                    }
                    // Note that there are no fixed limits below, but in the worst case it should be close to 150 chars -- i.e.: (100 + 35 + len(" and xxx more") 13 + 2)
                    String str = argumentsSplit[i];
                    if (str.length() > 35) {
                        argsWithTests.append(str.substring(0, 30));
                        argsWithTests.append(" ... ");
                    } else {
                        argsWithTests.append(str);
                    }
                    if (argsWithTests.length() > 100) {
                        argsWithTests.append(" and " + (argumentsSplit.length - (i + 1)) + " more");
                        break;
                    }
                }
                argsWithTests.append(" )");

                // then rename it to include the tests in the name
                // but first make sure the name is unique, as otherwise
                // configurations could get overwritten

                ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
                workingCopy.rename(manager.generateLaunchConfigurationName(argsWithTests.toString()));
                return workingCopy;
            }
            return workingCopy;
        }

        @Override
        protected List<ILaunchConfiguration> findExistingLaunchConfigurations(FileOrResource[] file) {
            List<ILaunchConfiguration> ret = new ArrayList<ILaunchConfiguration>();

            List<ILaunchConfiguration> existing = super.findExistingLaunchConfigurations(file);
            for (ILaunchConfiguration launch : existing) {
                boolean matches = false;
                try {
                    matches = launch.getAttribute(Constants.ATTR_UNITTEST_TESTS, "").equals(arguments);
                } catch (CoreException e) {
                    //ignore
                }
                if (matches) {
                    ret.add(launch);
                }
            }
            return ret;
        }
    }

    @Override
    public void run(IAction action) {

        PyEdit pyEdit = getPyEdit();
        final Tuple<String, IInterpreterManager> launchConfigurationTypeAndInterpreterManager = this
                .getLaunchConfigurationTypeAndInterpreterManager(pyEdit, true);

        Shell shell = EditorUtils.getShell();
        final DialogMemento memento = new DialogMemento(shell,
                "org.python.pydev.debug.ui.actions.RunEditorAsCustomUnitTestAction");
        SimpleNode ast = pyEdit.getAST();
        final ShiftListener shiftListener = new ShiftListener();
        Display d = shell.getDisplay();
        d.addFilter(SWT.KeyDown, shiftListener);
        d.addFilter(SWT.KeyUp, shiftListener);

        try {
            final TreeSelectionDialog dialog = new TreeSelectionDialog(shell,
                    new SelectTestLabelProvider(),
                    new SelectTestTreeContentProvider(pyEdit)) {

                private Label labelShiftToDebug;

                @Override
                public boolean close() {
                    memento.writeSettings(getShell());
                    return super.close();
                }

                @Override
                public Control createDialogArea(Composite parent) {
                    memento.readSettings();
                    Control ret = super.createDialogArea(parent);
                    ret.addTraverseListener(new TraverseListener() {

                        @Override
                        public void keyTraversed(TraverseEvent e) {
                            if (e.detail == SWT.TRAVERSE_RETURN) {
                                okPressed();
                            }
                        }
                    });
                    return ret;
                }

                /* (non-Javadoc)
                 * @see org.python.pydev.ui.dialogs.TreeSelectionDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
                 */
                @Override
                protected Control createButtonBar(Composite parent) {
                    Composite buttonBar = new Composite(parent, 0);

                    GridLayout layout = new GridLayout();
                    layout.numColumns = 2;
                    buttonBar.setLayout(layout);

                    GridData data = new GridData();
                    data.horizontalAlignment = SWT.FILL;
                    data.grabExcessHorizontalSpace = true;
                    buttonBar.setLayoutData(data);

                    Link configTestRunner = new Link(buttonBar, SWT.PUSH);
                    configTestRunner.setText(" <a>Configure test runner</a>");
                    configTestRunner.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            PyUnitPrefsPage2.showPage();
                        }
                    });

                    data = new GridData();
                    data.horizontalAlignment = GridData.BEGINNING;
                    data.grabExcessHorizontalSpace = true;
                    configTestRunner.setLayoutData(data);

                    labelShiftToDebug = new Label(buttonBar, 0);
                    labelShiftToDebug.setText("Run: Normal   (Press Shift to Debug)");
                    data = new GridData();
                    data.horizontalAlignment = GridData.END;
                    data.grabExcessHorizontalSpace = true;
                    labelShiftToDebug.setLayoutData(data);

                    shiftListener.onChanged.registerListener(new ICallbackListener<Boolean>() {

                        @Override
                        public Object call(Boolean shiftPressed) {
                            if (shiftPressed) {
                                labelShiftToDebug.setText("Run: Debug   (Release Shift for Normal)");
                            } else {
                                labelShiftToDebug.setText("Run: Normal   (Press Shift to Debug)");
                            }
                            labelShiftToDebug.getParent().layout(true);
                            return null;
                        }
                    });

                    Tree tree = getTreeViewer().getTree();
                    Menu menu = new Menu(tree.getShell(), SWT.POP_UP);
                    MenuItem runItem = new MenuItem(menu, SWT.PUSH);

                    final TreeSelectionDialog outerDialog = this;
                    runItem.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            ILaunchConfiguration conf = null;
                            IStructuredSelection selection = (IStructuredSelection) getTreeViewer().getSelection();

                            IEditorInput editorInput = pyEdit.getEditorInput();
                            IFile resource = editorInput != null ? editorInput.getAdapter(IFile.class) : null;
                            FileOrResource[] fileOrResource;
                            if (resource != null) {
                                fileOrResource = new FileOrResource[] { new FileOrResource(resource) };

                            } else {
                                fileOrResource = new FileOrResource[] { new FileOrResource(pyEdit.getEditorFile()) };
                            }

                            String testNames = getFullArgumentsRepresentation(selection.toArray());
                            UnittestLaunchShortcut shortcut = new UnittestLaunchShortcut(
                                    launchConfigurationTypeAndInterpreterManager,
                                    testNames);
                            List<ILaunchConfiguration> configurations = shortcut
                                    .findExistingLaunchConfigurations(fileOrResource);

                            boolean newConfiguration = false;
                            if (configurations.isEmpty()) {
                                conf = shortcut.createDefaultLaunchConfiguration(fileOrResource);
                                newConfiguration = true;
                            } else {
                                // assume that there's only one matching configuration
                                conf = configurations.get(0);
                            }

                            int retVal = DebugUITools.openLaunchConfigurationDialog(shell, conf,
                                    shiftListener.shiftPressed ? IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP
                                            : IDebugUIConstants.ID_RUN_LAUNCH_GROUP,
                                    null);

                            if (retVal == Window.CANCEL && newConfiguration) {
                                // user cancelled operation on newly created configuration
                                try {
                                    conf.delete();
                                } catch (CoreException e1) {
                                    // ignore
                                }
                            }
                            if (retVal == Window.OK) {
                                outerDialog.cancel();
                            }

                        }
                    });
                    runItem.setText("Customize run configuration...");

                    tree.setMenu(menu);

                    return buttonBar;
                }

                @Override
                protected Point getInitialSize() {
                    return memento.getInitialSize(super.getInitialSize(), getShell());
                }

                @Override
                protected Point getInitialLocation(Point initialSize) {
                    return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
                }

                /*
                 * @see SelectionStatusDialog#computeResult()
                 */
                @Override
                @SuppressWarnings("unchecked")
                protected void computeResult() {
                    doFinalUpdateBeforeComputeResult();

                    IStructuredSelection selection = (IStructuredSelection) getTreeViewer().getSelection();
                    List<Object> list = selection.toList();
                    if (list.size() > 0) {
                        setResult(list);
                    } else {
                        Tree tree = getTreeViewer().getTree();
                        TreeItem[] items = tree.getItems();
                        list = new ArrayList<Object>();
                        //Now, if he didn't select anything, let's create tests with all that is currently filtered
                        //in the interface
                        createListWithLeafs(items, list);
                        setResult(list);
                    }
                }

                private void createListWithLeafs(TreeItem[] items, List<Object> leafObjectsList) {
                    for (TreeItem item : items) {
                        TreeItem[] children = item.getItems();
                        if (children.length == 0) {
                            leafObjectsList.add(item.getData());
                        } else {
                            createListWithLeafs(children, leafObjectsList);
                        }
                    }
                }

            };

            dialog.setTitle("PyDev: Select tests to run");
            dialog.setMessage("Select the tests to run (press enter to run tests shown/selected)");

            PySelection ps = pyEdit.createPySelection();
            String selectedText;
            try {
                selectedText = ps.getSelectedText();
            } catch (BadLocationException e) {
                selectedText = "";
            }
            if (selectedText.length() > 0 && PyStringUtils.isValidIdentifier(selectedText, false)) {
                dialog.setInitialFilter(selectedText + " "); //Space in the end == exact match
            } else {
                dialog.setInitialFilter("test");
            }

            dialog.setAllowMultiple(true);
            dialog.setInput(ast);
            int open = dialog.open();
            if (open != Window.OK) {
                return;
            }
            Object[] result = dialog.getResult();

            final String arguments = getFullArgumentsRepresentation(result);

            AbstractLaunchShortcut shortcut = new UnittestLaunchShortcut(launchConfigurationTypeAndInterpreterManager,
                    arguments);

            if (shiftListener.shiftPressed) {
                shortcut.launch(pyEdit, "debug");
            } else {
                shortcut.launch(pyEdit, "run");
            }
        } finally {
            d.removeFilter(SWT.KeyDown, shiftListener);
            d.removeFilter(SWT.KeyUp, shiftListener);
        }
    }

    private String getFullArgumentsRepresentation(Object[] result) {
        final FastStringBuffer buf = new FastStringBuffer();
        if (result != null && result.length > 0) {

            for (Object o : result) {
                ASTEntry entry = (ASTEntry) o;
                if (entry.node instanceof ClassDef) {
                    if (buf.length() > 0) {
                        buf.append(',');
                    }
                    buf.append(NodeUtils.getFullRepresentationString(entry.node));

                } else if (entry.node instanceof FunctionDef && entry.parent == null) {
                    if (buf.length() > 0) {
                        buf.append(',');
                    }
                    buf.append(NodeUtils.getFullRepresentationString(entry.node));

                } else if (entry.node instanceof FunctionDef && entry.parent != null
                        && entry.parent.node instanceof ClassDef) {
                    if (buf.length() > 0) {
                        buf.append(',');
                    }
                    buf.append(NodeUtils.getFullRepresentationString(entry.parent.node));
                    buf.append('.');
                    buf.append(NodeUtils.getFullRepresentationString(entry.node));

                }

            }
        }

        final String arguments;
        if (buf.length() > 0) {
            arguments = buf.toString();
        } else {
            arguments = "";
        }
        return arguments;
    }

}

final class SelectTestLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object element) {
        SimpleNode n = ((ASTEntry) element).node;
        if (n instanceof ClassDef) {
            return ImageCache.asImage(PyCodeCompletionImages.getImageForType(IToken.TYPE_CLASS));
        }
        if (n instanceof FunctionDef) {
            return ImageCache.asImage(PyCodeCompletionImages.getImageForType(IToken.TYPE_FUNCTION));
        }
        return ImageCache.asImage(PyCodeCompletionImages.getImageForType(IToken.TYPE_ATTR));
    }

    @Override
    public String getText(Object element) {
        return NodeUtils.getFullRepresentationString(((ASTEntry) element).node);
    }
}

final class SelectTestTreeContentProvider implements ITreeContentProvider {

    private EasyASTIteratorVisitor visitor;
    private Map<Object, ASTEntry[]> cache = new HashMap<Object, ASTEntry[]>();
    private PyEdit pyEdit;

    public SelectTestTreeContentProvider(PyEdit pyEdit) {
        this.pyEdit = pyEdit;
    }

    @Override
    public Object[] getChildren(Object element) {
        Object[] ret = cache.get(element);
        if (ret != null) {
            return ret;
        }

        ASTEntry entry = (ASTEntry) element;

        //Only get classes and 1st level methods for tests.
        if (entry.node instanceof ClassDef) {
            Iterator<ASTEntry> it = visitor.getMethodsIterator();
            ArrayList<ASTEntry> list = new ArrayList<ASTEntry>();
            while (it.hasNext()) {
                ASTEntry next = it.next();
                if (next.parent != null && next.parent.node == entry.node) {
                    list.add(next);
                }
            }
            ASTEntry[] array = list.toArray(new ASTEntry[0]);
            cache.put(element, array);
            return array;
        }

        return null;
    }

    @Override
    public Object getParent(Object element) {
        ASTEntry entry = (ASTEntry) element;
        return entry.parent;
    }

    @Override
    public boolean hasChildren(Object element) {
        ASTEntry entry = (ASTEntry) element;

        if (entry.node instanceof ClassDef) {
            Object[] children = getChildren(entry);
            return children != null && children.length > 0;
        }
        return false;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        visitor = EasyASTIteratorVisitor.create((SimpleNode) inputElement);
        if (visitor == null) {
            return new Object[0];
        }

        //Get the top-level classes
        Iterator<ASTEntry> it = visitor.getClassesIterator();
        ArrayList<ASTEntry> list = new ArrayList<ASTEntry>();
        while (it.hasNext()) {
            ASTEntry next = it.next();
            if (next.parent == null) {
                list.add(next);
            }
        }

        if (PyUnitPrefsPage2.isPyTestRun(this.pyEdit)) {
            // We'll only add methods which are top-level when in the py.test run (which accepts those, as
            // the regular unit-test runner doesn't accept it).
            it = visitor.getMethodsIterator();
            while (it.hasNext()) {
                ASTEntry next = it.next();
                if (next.parent == null) {
                    list.add(next);
                }
            }
        }
        return list.toArray(new ASTEntry[0]);
    }

    @Override
    public void dispose() {
        //do nothing
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        //do nothing
    }
}
