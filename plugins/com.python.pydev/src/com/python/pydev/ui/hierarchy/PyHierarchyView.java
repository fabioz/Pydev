/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.ui.hierarchy;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;
import org.python.pydev.shared_ui.utils.IViewWithControls;
import org.python.pydev.ui.NotifyViewCreated;
import org.python.pydev.ui.ViewPartWithOrientation;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class PyHierarchyView extends ViewPartWithOrientation implements IViewWithControls {

    public static final String PYHIERARCHY_VIEW_ORIENTATION = "PYHIERARCHY_VIEW_ORIENTATION";

    private final HierarchyViewer viewer = new HierarchyViewer();

    public PyHierarchyView() {
        NotifyViewCreated.notifyViewCreated(this);

    }

    @Override
    public void createPartControl(Composite parent) {
        viewer.createPartControl(parent);

        IActionBars actionBars = getViewSite().getActionBars();
        IMenuManager menuManager = actionBars.getMenuManager();
        addOrientationPreferences(menuManager);

        onControlCreated.call(viewer.treeClassesViewer);
        onControlCreated.call(viewer.treeMembers);
    }

    public void setHierarchy(HierarchyNodeModel model) {
        viewer.setHierarchy(model);
    }

    @Override
    public void setFocus() {
        viewer.setFocus();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (viewer.treeClassesViewer != null && !viewer.treeClassesViewer.getTree().isDisposed()) {
            onControlDisposed.call(viewer.treeClassesViewer);
        }
        if (viewer.treeMembers != null && !viewer.treeMembers.isDisposed()) {
            onControlDisposed.call(viewer.treeMembers);
        }
        viewer.dispose();

    }

    @Override
    public String getOrientationPreferencesKey() {
        return PYHIERARCHY_VIEW_ORIENTATION;
    }

    @Override
    protected void setNewOrientation(int orientation) {
        viewer.setNewOrientation(orientation);
    }

    @Override
    public ICallbackWithListeners getOnControlCreated() {
        return onControlCreated;
    }

    @Override
    public ICallbackWithListeners getOnControlDisposed() {
        return onControlDisposed;
    }
}
