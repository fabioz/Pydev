/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.tree.PyFilteredTree;

public class ProjectSelectionDialog extends SelectionStatusDialog {

    private TreeViewer fTreeViewer;

    private final static int WIDGET_HEIGHT = 250;
    private final static int WIDGET_WIDTH = 300;

    private String natureId;

    private PatternFilter patternFilter;

    private PyFilteredTree filteredTree;

    private boolean multipleSelection;

    /**
     * May be set by the user to show projects differently (default is WorkbenchLabelProvider).
     * Must be set before the dialog is opened.
     */
    public IBaseLabelProvider labelProvider;

    public ProjectSelectionDialog(Shell parentShell, String natureId) {
        this(parentShell, natureId, false);
    }

    public ProjectSelectionDialog(Shell parentShell, String natureId, boolean multipleSelection) {
        super(parentShell);
        this.labelProvider = new WorkbenchLabelProvider();
        setTitle("Select project");
        setMessage("Select project");
        this.multipleSelection = multipleSelection;
        this.natureId = natureId;
        int shellStyle = getShellStyle();
        setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        // page group
        Composite composite = (Composite) super.createDialogArea(parent);

        Font font = parent.getFont();
        composite.setFont(font);

        createMessageArea(composite);

        patternFilter = new PatternFilter();
        filteredTree = PyFilteredTree.create(composite, patternFilter, true);

        fTreeViewer = filteredTree.getViewer();
        fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                doSelectionChanged(((IStructuredSelection) event.getSelection()).toArray());
            }
        });
        fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                okPressed();
            }
        });
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = WIDGET_HEIGHT;
        data.widthHint = WIDGET_WIDTH;
        fTreeViewer.getTree().setLayoutData(data);

        fTreeViewer.setLabelProvider(labelProvider);
        fTreeViewer.setContentProvider(new ArrayContentProvider());

        fTreeViewer.getControl().setFont(font);

        if (natureId != null) {
            fTreeViewer.addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if (element instanceof IProject) {
                        IProject project = (IProject) element;
                        try {
                            return project.isOpen() && project.hasNature(natureId);
                        } catch (CoreException e) {
                            return false;
                        }
                    }
                    return true;
                }
            });
        }

        IProject[] input = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        fTreeViewer.setInput(input);

        doSelectionChanged(new Object[0]);
        Dialog.applyDialogFont(composite);
        SharedUiPlugin.setCssId(parent, "py-project-selection-dialog", true);
        return composite;
    }

    private void doSelectionChanged(Object[] objects) {
        if (multipleSelection) {
            if (objects.length == 0) {
                updateStatus(new Status(IStatus.ERROR, "org.python.pydev.shared_ui", "Select one or more projects")); //$NON-NLS-1$
                setSelectionResult(null);
            } else {
                updateStatus(new Status(IStatus.OK, "org.python.pydev.shared_ui", objects.length + " selected"));
                setSelectionResult(objects);
            }
        } else {
            if (objects.length != 1) {
                updateStatus(new Status(IStatus.ERROR, "org.python.pydev.shared_ui", "Select one project")); //$NON-NLS-1$
                setSelectionResult(null);
            } else {
                updateStatus(new Status(IStatus.OK, "org.python.pydev.shared_ui", objects.length + " selected"));
                setSelectionResult(objects);
            }
        }
    }

    @Override
    protected void updateStatus(IStatus status) {
        super.updateStatus(status);
        Control area = this.getDialogArea();
        if (area != null) {
            SharedUiPlugin.fixSelectionStatusDialogStatusLineColor(this, area.getBackground());
        }
    }

    /**
     * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
     */
    @Override
    protected void computeResult() {
        Tree tree = fTreeViewer.getTree();
        TreeItem[] selection = tree.getSelection();
        List<IProject> p = new ArrayList<>();
        for (TreeItem treeItem : selection) {
            Object data = treeItem.getData();
            if (data instanceof IProject) {
                p.add((IProject) data);
            }
        }
        if (p.size() == 0) {
            TreeItem[] items = tree.getItems();
            if (items.length > 0) {
                Object data = items[0].getData();
                if (data instanceof IProject) {
                    p.add((IProject) data);
                }
            }
        }
        setSelectionResult(p.toArray(new IProject[0]));
    }
}

final class ArrayContentProvider implements ITreeContentProvider {

    public Object[] getChildren(Object element) {
        if (element instanceof Object[]) {
            Object[] list = (Object[]) element;
            return list;
        }
        return new Object[0];
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object element) {
        return element instanceof Object[] && ((Object[]) element).length > 0;
    }

    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public void dispose() {
        //do nothing
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        //do nothing
    }
}