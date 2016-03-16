/******************************************************************************
* Copyright (C) 2011-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.outline;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.python.pydev.shared_ui.tree.PyFilteredTree;

/**
 * Base class for a viewer with a tree viewer and a filter.
 * 
 * Based on the ContentOutlinePage.
 */
public abstract class ContentOutlinePageWithFilter extends Page implements IContentOutlinePage,
        ISelectionChangedListener {
    private ListenerList selectionChangedListeners = new ListenerList();

    protected TreeViewer treeViewer;
    protected PatternFilter patternFilter;
    protected FilteredTree filter;

    @Override
    public void createControl(Composite parent) {
        patternFilter = new PatternFilter();
        filter = PyFilteredTree.create(parent, patternFilter, false);
        treeViewer = filter.getViewer();
        treeViewer.addSelectionChangedListener(this);
    }

    @Override
    public void init(IPageSite pageSite) {
        super.init(pageSite);
        pageSite.setSelectionProvider(this);
    }

    // Selection ---------------------------------------------------------------

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListeners.add(listener);
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListeners.remove(listener);
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        fireSelectionChanged(event.getSelection());
    }

    protected void fireSelectionChanged(ISelection selection) {
        final SelectionChangedEvent event = new SelectionChangedEvent(this, selection);

        Object[] listeners = selectionChangedListeners.getListeners();
        for (int i = 0; i < listeners.length; ++i) {
            final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
            SafeRunner.run(new SafeRunnable() {
                @Override
                public void run() {
                    l.selectionChanged(event);
                }
            });
        }
    }

    @Override
    public ISelection getSelection() {
        if (treeViewer == null) {
            return StructuredSelection.EMPTY;
        }
        return treeViewer.getSelection();
    }

    @Override
    public Control getControl() {
        if (filter == null) {
            return null;
        }
        return filter;
    }

    public TreeViewer getTreeViewer() {
        return treeViewer;
    }

    @Override
    public void setFocus() {
        filter.getFilterControl().setFocus();
    }

    @Override
    public void setSelection(ISelection selection) {
        if (treeViewer != null) {
            treeViewer.setSelection(selection);
        }
    }
}
