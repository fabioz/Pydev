/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import static org.python.pydev.navigator.PythonBaseModelProvider.DEBUG;

import java.util.List;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.internal.navigator.workingsets.WorkingSetsContentProvider;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.python.pydev.core.log.Log;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.shared_core.callbacks.ICallback;

/**
 * Based on code from WorkingSetsContentProvider (but as it's internal and dependent on ProjectExplorer, 
 * we MUST create our own)
 * 
 * @author Fabio
 */
@SuppressWarnings("restriction")
public class TopLevelProjectsOrWorkingSetChoice {

    /**
     * This is the constant indicating the property we should hear to know what we should show as top-level elements.
     */
    private final static String SHOW_TOP_LEVEL_WORKING_SETS = WorkingSetsContentProvider.SHOW_TOP_LEVEL_WORKING_SETS;

    /**
     * Constant used to indicate that the rootMode is to show working sets as top-level elements
     * @see #getRootMode()
     */
    public static final int WORKING_SETS = 0;

    /**
     * Constant used to indicate that the rootMode is to show projects sets as top-level elements
     * @see #getRootMode()
     */
    public static final int PROJECTS = 1;

    /**
     * This is the extension where we register to listen to property changes.
     */
    private IExtensionStateModel extensionStateModel;

    /**
     * Listens to property changes and updates which should be the top-level elements to be shown.
     */
    private IPropertyChangeListener rootModeListener = new IPropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (DEBUG) {
                System.out.println("Property change: " + event.getProperty());
            }
            if (SHOW_TOP_LEVEL_WORKING_SETS.equals(event.getProperty())) {
                updateRootMode();
            }
        }

    };

    /**
     * Starts listening to property changes related to which should be the top-level elements to be shown.
     */
    public void init(ICommonContentExtensionSite aConfig, CommonViewer viewer) {
        //if it had something, dispose of its association!
        this.dispose();
        try {
            extensionStateModel = viewer.getNavigatorContentService().findStateModel(
                    WorkingSetsContentProvider.EXTENSION_ID);

            extensionStateModel.addPropertyChangeListener(rootModeListener);
        } catch (Exception e) {
            Log.log(e);
        }
        updateRootMode();
    }

    /**
     * This method should be called to update the internal variable indicating whether we should show working sets
     * as top level or just the projects.
     */
    private void updateRootMode() {
        if (extensionStateModel != null) {
            if (extensionStateModel.getBooleanProperty(SHOW_TOP_LEVEL_WORKING_SETS)) {
                //show working set
                this.rootMode = WORKING_SETS;
                if (DEBUG) {
                    System.out.println("Show working set as top level");
                }
            } else {
                //show projects
                this.rootMode = PROJECTS;
                if (DEBUG) {
                    System.out.println("Show projects as top level");
                }
            }
        }
    }

    /**
     * Stops listening to property changes.
     */
    public void dispose() {
        try {
            if (extensionStateModel != null) {
                extensionStateModel.removePropertyChangeListener(rootModeListener);
            }
            extensionStateModel = null;
        } catch (Exception e) {
            Log.log(e);
        }
    }

    protected int rootMode = PROJECTS;

    /**
     * @see #PROJECTS
     * @see #WORKING_SETS
     */
    public int getRootMode() {
        return rootMode;
    }

    /**
     * @param object the object whose parent we want
     * @param getWorkingSetsCallback a callback that'll return the available working sets.
     * @return null if the parent is not a working set element or the working set that's a parent
     * @note if we're not currently showing working sets as top-level elements, it'll return null. 
     */
    public Object getWorkingSetParentIfAvailable(Object object,
            ICallback<List<IWorkingSet>, IWorkspaceRoot> getWorkingSetsCallback) {

        if (rootMode != WORKING_SETS || object == null) {
            return null;
        }
        //TODO: This could be optimized by creating an auxiliary structure where child->parent working set
        //so that we could get it directly without the need to traverse all the elements.
        //this can be interesting because whenever we try to get a parent this method will be called
        //for all the elements.

        //showing as working sets
        List<IWorkingSet> workingSets = getWorkingSetsCallback.call(null);
        for (IWorkingSet w : workingSets) {
            IAdaptable[] elements = w.getElements();
            if (elements != null) {
                for (IAdaptable a : elements) {
                    if (a == null) {
                        continue;
                    }
                    if (object.equals(a)) {
                        return w;
                    }
                    if (object instanceof IWrappedResource) {
                        IWrappedResource wrappedResource = (IWrappedResource) object;
                        if (wrappedResource.getActualObject().equals(a)) {
                            return w;
                        }
                    }
                }
            }
        }
        return null;
    }

}
