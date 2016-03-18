/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.outline;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.shared_ui.ImageCache;

/**
 * This class is provided as a base class for filters acting in the outline nodes.
 *
 * @author Fabio
 */
public abstract class AbstractOutlineFilterAction extends Action {

    protected ViewerFilter filter;

    protected WeakReference<BaseOutlinePage> page;

    protected String preference;

    /**
     * @param text This is the text explaining this action (shown to the user)
     * @param page the outline page where the action should take place
     * @param imageCache the image cache to be used to get the icon
     * @param preference the name of the preference that should keep the preferences (to keep it checked among sessions)
     * @param uiConstant the name of the constant in UIConstants to get the icon from the image cache.
     */
    public AbstractOutlineFilterAction(String text, BaseOutlinePage page, ImageCache imageCache, String preference,
            String uiConstant) {
        super(text, IAction.AS_CHECK_BOX);
        this.preference = preference;

        this.page = new WeakReference<BaseOutlinePage>(page);

        setChecked(page.getStore().getBoolean(preference));
        setActionEnabled(isChecked());

        setImageDescriptor(imageCache.getDescriptor(uiConstant));
    }

    @Override
    public void run() {
        setActionEnabled(isChecked());
    }

    /**
     * Should we hide things?
     */
    protected void setActionEnabled(boolean enableAction) {
        BaseOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(preference, enableAction);
            if (filter == null) {
                filter = createFilter();
            }

            TreeViewer treeViewer = p.getTreeViewer();
            if (treeViewer != null) {
                if (enableAction) {
                    treeViewer.addFilter(filter);
                } else {
                    treeViewer.removeFilter(filter);
                }
            }
        }
    }

    /**
     * Subclasses should implement this method to return a filter to filter whatever it should target.
     */
    protected abstract ViewerFilter createFilter();

}
