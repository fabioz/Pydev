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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.action.IAction;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.debug.ui.launching.FileOrResource;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.PyCodeCompletionImages;
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
import org.python.pydev.shared_ui.dialogs.DialogMemento;
import org.python.pydev.ui.dialogs.TreeSelectionDialog;

class ShiftListener implements Listener {

    public boolean shiftPressed = false;
    public CallbackWithListeners<Boolean> onChanged = new CallbackWithListeners<>();

    public ShiftListener() {
    }

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
            TreeSelectionDialog dialog = new TreeSelectionDialog(shell, new SelectTestLabelProvider(),
                    new SelectTestTreeContentProvider()) {

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
            String selectedText = ps.getSelectedText();
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

            AbstractLaunchShortcut shortcut = new AbstractLaunchShortcut() {

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
                    ILaunchConfigurationWorkingCopy workingCopy = super
                            .createDefaultLaunchConfigurationWithoutSaving(resource);
                    if (arguments.length() > 0) {
                        workingCopy.setAttribute(Constants.ATTR_UNITTEST_TESTS, arguments);
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
            };

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

}

final class SelectTestLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object element) {
        SimpleNode n = ((ASTEntry) element).node;
        if (n instanceof ClassDef) {
            return PyCodeCompletionImages.getImageForType(IToken.TYPE_CLASS);
        }
        if (n instanceof FunctionDef) {
            return PyCodeCompletionImages.getImageForType(IToken.TYPE_FUNCTION);
        }
        return PyCodeCompletionImages.getImageForType(IToken.TYPE_ATTR);
    }

    @Override
    public String getText(Object element) {
        return NodeUtils.getFullRepresentationString(((ASTEntry) element).node);
    }
}

final class SelectTestTreeContentProvider implements ITreeContentProvider {

    private EasyASTIteratorVisitor visitor;
    private Map<Object, ASTEntry[]> cache = new HashMap<Object, ASTEntry[]>();

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

    public Object getParent(Object element) {
        ASTEntry entry = (ASTEntry) element;
        return entry.parent;
    }

    public boolean hasChildren(Object element) {
        ASTEntry entry = (ASTEntry) element;

        if (entry.node instanceof ClassDef) {
            Object[] children = getChildren(entry);
            return children != null && children.length > 0;
        }
        return false;
    }

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

        if (PyUnitPrefsPage2.isPyTestRun()) {
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

    public void dispose() {
        //do nothing
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        //do nothing
    }
}
