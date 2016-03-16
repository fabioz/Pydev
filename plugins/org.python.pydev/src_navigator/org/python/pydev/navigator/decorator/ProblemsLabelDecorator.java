/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.decorator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.UIConstants;


/**
 * Decorates problems.
 * 
 * Based on: org.eclipse.jdt.ui.ProblemsLabelDecorator
 */
public class ProblemsLabelDecorator implements ILabelDecorator, ILightweightLabelDecorator {

    private IProblemChangedListener fProblemChangedListener;
    private ListenerList fListeners;

    /**
     * Creates a new <code>ProblemsLabelDecorator</code>.
     */
    public ProblemsLabelDecorator() {
    }

    /* (non-Javadoc)
     * @see ILabelDecorator#decorateText(String, Object)
     */
    @Override
    public String decorateText(String text, Object element) {
        return text;
    }

    /* (non-Javadoc)
     * @see ILabelDecorator#decorateImage(Image, Object)
     */
    @Override
    public Image decorateImage(Image image, Object obj) {
        Log.log("Did not expect this module to be called -- implementing org.eclipse.jface.viewers.ILightweightLabelDecorator.");
        return image;
    }

    protected int getErrorState(Object obj) {
        try {
            if (obj instanceof IResource) {
                return getErrorTicksFromMarkers((IResource) obj, IResource.DEPTH_INFINITE);
            } else if (obj instanceof IAdaptable) {
                IResource resource = (IResource) ((IAdaptable) obj).getAdapter(IResource.class);
                if (resource != null) {
                    return getErrorTicksFromMarkers(resource, IResource.DEPTH_INFINITE);
                }
            }
        } catch (CoreException e) {
            Log.log(e);
        }
        return 0;
    }

    private int getErrorTicksFromMarkers(IResource res, int depth) throws CoreException {
        if (!res.isAccessible()) {
            return 0;
        }
        return res.findMaxProblemSeverity(IMarker.PROBLEM, true, depth);
    }

    /* (non-Javadoc)
     * @see IBaseLabelProvider#dispose()
     */
    @Override
    public void dispose() {
        if (fProblemChangedListener != null) {
            ProblemMarkerManager.getSingleton().removeListener(fProblemChangedListener);
            fProblemChangedListener = null;
        }
    }

    /* (non-Javadoc)
     * @see IBaseLabelProvider#isLabelProperty(Object, String)
     */
    @Override
    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    /* (non-Javadoc)
     * @see IBaseLabelProvider#addListener(ILabelProviderListener)
     */
    @Override
    public void addListener(ILabelProviderListener listener) {
        if (fListeners == null) {
            fListeners = new ListenerList();
        }
        fListeners.add(listener);
        if (fProblemChangedListener == null) {
            fProblemChangedListener = new IProblemChangedListener() {
                @Override
                public void problemsChanged(IResource[] changedResources, boolean isMarkerChange) {
                    if (fListeners != null && !fListeners.isEmpty()) {
                        LabelProviderChangedEvent event = new ProblemsLabelChangedEvent(ProblemsLabelDecorator.this,
                                changedResources, isMarkerChange);
                        Object[] listeners = fListeners.getListeners();
                        for (int i = 0; i < listeners.length; i++) {
                            ((ILabelProviderListener) listeners[i]).labelProviderChanged(event);
                        }
                    }
                }
            };
            ProblemMarkerManager.getSingleton().addListener(fProblemChangedListener);
        }
    }

    /* (non-Javadoc)
     * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
     */
    @Override
    public void removeListener(ILabelProviderListener listener) {
        if (fListeners != null) {
            fListeners.remove(listener);
            if (fProblemChangedListener != null && fListeners.isEmpty()) {
                ProblemMarkerManager.getSingleton().removeListener(fProblemChangedListener);
                fProblemChangedListener = null;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
     */
    @Override
    public void decorate(Object element, IDecoration decoration) {
        int errorState = getErrorState(element);
        if (errorState == IMarker.SEVERITY_ERROR) {
            decoration.addOverlay(PydevPlugin.getImageCache().getDescriptor(UIConstants.ERROR_DECORATION),
                    IDecoration.BOTTOM_LEFT);

        } else if (errorState == IMarker.SEVERITY_WARNING) {
            decoration.addOverlay(PydevPlugin.getImageCache().getDescriptor(UIConstants.WARNING_DECORATION),
                    IDecoration.BOTTOM_LEFT);
        }
    }

}
