/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.decorator;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.utils.UIUtils;

/**
 * Listens to resource deltas and filters for marker changes of type IMarker.PROBLEM
 * Viewers showing error ticks should register as listener to
 * this type.
 * 
 * Based on: org.eclipse.jdt.internal.ui.viewsupport.ProblemMarkerManager
 */
public class ProblemMarkerManager implements IResourceChangeListener {

    /**
     * Visitors used to look if the element change delta contains a marker change.
     */
    private static class ProjectErrorVisitor implements IResourceDeltaVisitor {

        private HashSet<IResource> fChangedElements;

        public ProjectErrorVisitor(HashSet<IResource> changedElements) {
            fChangedElements = changedElements;
        }

        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource res = delta.getResource();
            if (res instanceof IProject && delta.getKind() == IResourceDelta.CHANGED) {
                IProject project = (IProject) res;
                if (!project.isAccessible()) {
                    // only track open projects
                    return false;
                }
            }
            checkInvalidate(delta, res);
            return true;
        }

        private void checkInvalidate(IResourceDelta delta, IResource resource) {
            int kind = delta.getKind();
            if (kind == IResourceDelta.REMOVED || kind == IResourceDelta.ADDED
                    || (kind == IResourceDelta.CHANGED && isErrorDelta(delta))) {
                // invalidate the resource and all parents
                while (resource.getType() != IResource.ROOT && fChangedElements.add(resource)) {
                    resource = resource.getParent();
                }
            }
        }

        private boolean isErrorDelta(IResourceDelta delta) {
            if ((delta.getFlags() & IResourceDelta.MARKERS) != 0) {
                IMarkerDelta[] markerDeltas = delta.getMarkerDeltas();
                for (int i = 0; i < markerDeltas.length; i++) {
                    IMarkerDelta iMarkerDelta = markerDeltas[i];
                    if (iMarkerDelta.isSubtypeOf(IMarker.PROBLEM)) {
                        int kind = iMarkerDelta.getKind();
                        if (kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED) {
                            return true;
                        }
                        int severity = iMarkerDelta.getAttribute(IMarker.SEVERITY, -1);
                        int newSeverity = iMarkerDelta.getMarker().getAttribute(IMarker.SEVERITY, -1);
                        if (newSeverity != severity) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private ListenerList fListeners;

    private Set<IResource> fResourcesWithMarkerChanges;
    private Set<IResource> fResourcesWithAnnotationChanges;

    private UIJob fNotifierJob;

    private static ProblemMarkerManager fProblemMarkerManagerSingleton;

    /**
     * Singleton
     */
    protected ProblemMarkerManager() {
        fListeners = new ListenerList();
        fResourcesWithMarkerChanges = new HashSet<IResource>();
        fResourcesWithAnnotationChanges = new HashSet<IResource>();
    }

    public static synchronized ProblemMarkerManager getSingleton() {
        if (fProblemMarkerManagerSingleton == null) {
            fProblemMarkerManagerSingleton = new ProblemMarkerManager();
        }
        return fProblemMarkerManagerSingleton;
    }

    /*
     * @see IResourceChangeListener#resourceChanged
     */
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        HashSet<IResource> changedElements = new HashSet<IResource>();

        try {
            IResourceDelta delta = event.getDelta();
            if (delta != null) {
                delta.accept(new ProjectErrorVisitor(changedElements));
            }
        } catch (CoreException e) {
            Log.log(e);
        }

        if (!changedElements.isEmpty()) {
            boolean hasChanges = false;
            synchronized (this) {
                if (fResourcesWithMarkerChanges.isEmpty()) {
                    fResourcesWithMarkerChanges = changedElements;
                    hasChanges = true;
                } else {
                    hasChanges = fResourcesWithMarkerChanges.addAll(changedElements);
                }
            }
            if (hasChanges) {
                fireChanges();
            }
        }
    }

    /**
     * Adds a listener for problem marker changes.
     * @param listener the listener to add
     */
    public void addListener(IProblemChangedListener listener) {
        if (fListeners.isEmpty()) {
            ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        }
        fListeners.add(listener);
    }

    /**
     * Removes a <code>IProblemChangedListener</code>.
     * @param listener the listener to remove
     */
    public void removeListener(IProblemChangedListener listener) {
        fListeners.remove(listener);
        if (fListeners.isEmpty()) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        }
    }

    private void fireChanges() {
        Display display = UIUtils.getStandardDisplay();
        if (display != null && !display.isDisposed()) {
            postAsyncUpdate(display);
        }
    }

    private void postAsyncUpdate(final Display display) {
        if (fNotifierJob == null) {
            fNotifierJob = new UIJob(display, "Update problem marker decorations") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    //Yes, MUST be called on UI thread!
                    IResource[] markerResources = null;
                    IResource[] annotationResources = null;
                    synchronized (ProblemMarkerManager.this) {
                        if (!fResourcesWithMarkerChanges.isEmpty()) {
                            markerResources = fResourcesWithMarkerChanges
                                    .toArray(new IResource[fResourcesWithMarkerChanges.size()]);
                            fResourcesWithMarkerChanges.clear();
                        }
                        if (!fResourcesWithAnnotationChanges.isEmpty()) {
                            annotationResources = fResourcesWithAnnotationChanges
                                    .toArray(new IResource[fResourcesWithAnnotationChanges.size()]);
                            fResourcesWithAnnotationChanges.clear();
                        }
                    }
                    Object[] listeners = fListeners.getListeners();
                    for (int i = 0; i < listeners.length; i++) {
                        IProblemChangedListener curr = (IProblemChangedListener) listeners[i];
                        if (markerResources != null) {
                            curr.problemsChanged(markerResources, true);
                        }
                        if (annotationResources != null) {
                            curr.problemsChanged(annotationResources, false);
                        }
                    }
                    return Status.OK_STATUS;
                }
            };
            fNotifierJob.setSystem(true);
            fNotifierJob.setPriority(UIJob.DECORATE);
        }
        fNotifierJob.schedule(10);
    }

}
