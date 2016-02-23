/******************************************************************************
* Copyright (C) 2012  Jonah Graham
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.debug.ui.variables;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariablesPreferences;

/** 
 * Based on org.eclipse.jdt.internal.debug.ui.heapwalking.AllReferencesInViewActionDelegate and similar
 * classes that provide similar filtering functionality for java debugging
 */
abstract public class AbstractShowReferencesActionDelegate extends ViewerFilter implements IPropertyChangeListener,
        IActionDelegate2,
        IViewActionDelegate {

    private IAction fAction;
    private IViewPart fView;

    protected StructuredViewer getStructuredViewer() {
        if (fView == null) {
            return null;
        }
        IDebugView view = (IDebugView) fView.getAdapter(IDebugView.class);
        if (view != null) {
            Viewer viewer = view.getViewer();
            if (viewer instanceof StructuredViewer) {
                return (StructuredViewer) viewer;
            }
        }
        return null;
    }

    public AbstractShowReferencesActionDelegate() {
        super();
    }

    @Override
    public void run(IAction action) {
        boolean checked = action.isChecked();
        setShowReferences(checked);
        StructuredViewer structuredViewer = getStructuredViewer();
        if (structuredViewer != null) {
            ViewerFilter[] filters = structuredViewer.getFilters();
            boolean alreadyAdded = false;
            for (ViewerFilter filter : filters) {
                if (filter == this) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                structuredViewer.addFilter(this);
            } else {
                // addFilter (above) does a refresh, so only
                // force a refresh if we aren't adding
                structuredViewer.refresh();
            }
        }
    }

    @Override
    public void init(IAction action) {
        fAction = action;
        action.setChecked(isShowReference());
        PyVariablesPreferences.addPropertyChangeListener(this);
        run(fAction);
    }

    @Override
    public void init(IViewPart view) {
        fView = view;
        run(fAction);
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    @Override
    public void dispose() {
        PyVariablesPreferences.removePropertyChangeListener(this);
    }

    @Override
    public void runWithEvent(IAction action, Event event) {
        run(action);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (isShowReferenceProperty(property)) {
            if (fAction != null) {
                fAction.setChecked(isShowReference());
                run(fAction);
            }
        }
    }

    /**
     * Return true if property (as a string) refers to the property being tracked.
     * @param property
     * @return
     */
    abstract protected boolean isShowReferenceProperty(String property);

    /**
     * Return true if the property is true
     * @return
     */
    abstract protected boolean isShowReference();

    /**
     * Set the property
     * @param checked
     */
    abstract protected void setShowReferences(boolean checked);

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (isShowReference()) {
            return true;
        } else {
            if (element instanceof PyVariable) {
                PyVariable variable = (PyVariable) element;
                try {
                    String name = variable.getName();
                    return select(viewer, parentElement, variable, name);
                } catch (DebugException e) {
                    // Ignore error, if we get one, don't filter
                }
            }

            return true;
        }
    }

    /**
     * Convenience method that is called by {@link #select(Viewer, Object, Object)} with the PyVariable
     * extracted
     */
    protected boolean select(Viewer viewer, Object parentElement, PyVariable variable, String variableName) {
        return true;
    }
}
