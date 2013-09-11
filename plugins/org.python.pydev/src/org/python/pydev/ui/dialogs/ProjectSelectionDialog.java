/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
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
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.plugin.StatusInfo;
import org.python.pydev.shared_ui.tree.PyFilteredTree;

public class ProjectSelectionDialog extends SelectionStatusDialog {

    private TreeViewer fTreeViewer;

    private final static int WIDGET_HEIGHT = 250;
    private final static int WIDGET_WIDTH = 300;

    private String natureId;

    private PatternFilter patternFilter;

    private PyFilteredTree filteredTree;

    public ProjectSelectionDialog(Shell parentShell, String natureId) {
        super(parentShell);
        setTitle("Select project");
        setMessage("Select project");
        this.natureId = natureId;
        int shellStyle = getShellStyle();
        setShellStyle(shellStyle | SWT.MAX | SWT.RESIZE);
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
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

        fTreeViewer.setLabelProvider(new WorkbenchLabelProvider());
        fTreeViewer.setContentProvider(new ArrayContentProvider());

        fTreeViewer.getControl().setFont(font);

        if (natureId != null) {
            fTreeViewer.addFilter(new ViewerFilter() {
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
        return composite;
    }

    private void doSelectionChanged(Object[] objects) {
        if (objects.length != 1) {
            updateStatus(new StatusInfo(IStatus.ERROR, "")); //$NON-NLS-1$
            setSelectionResult(null);
        } else {
            updateStatus(new StatusInfo());
            setSelectionResult(objects);
        }
    }

    /**
     * @see org.eclipse.ui.dialogs.SelectionStatusDialog#computeResult()
     */
    protected void computeResult() {
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