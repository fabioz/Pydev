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
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;

/**
 * Action that addds a way to sort items by the name.
 * 
 * @author Fabio
 */
public class OutlineSortByNameAction extends Action {

    private final String PREF_ALPHA_SORT;

    ViewerSorter sortByNameSorter;

    private WeakReference<BaseOutlinePage> page;

    public OutlineSortByNameAction(BaseOutlinePage page, ImageCache imageCache, String pluginId) {
        super("Sort by name", IAction.AS_CHECK_BOX);
        PREF_ALPHA_SORT = pluginId + ".OUTLINE_ALPHA_SORT";
        this.page = new WeakReference<BaseOutlinePage>(page);

        setChecked(page.getStore().getBoolean(PREF_ALPHA_SORT));
        setAlphaSort(isChecked());

        setImageDescriptor(imageCache.getDescriptor(UIConstants.ALPHA_SORT));
        setToolTipText("Sort by name");
    }

    /**
     * @param doSort :
     *            sort or not?
     */
    public void setAlphaSort(boolean doSort) {
        BaseOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(PREF_ALPHA_SORT, doSort);
            if (sortByNameSorter == null) {
                sortByNameSorter = new ViewerSorter() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public int compare(Viewer viewer, Object e1, Object e2) {
                        return ((Comparable<Object>) e1).compareTo(e2);
                    }
                };
            }
            p.getTreeViewer().setSorter(doSort ? sortByNameSorter : null);
        }
    }

    @Override
    public void run() {
        setAlphaSort(isChecked());
    }

}
