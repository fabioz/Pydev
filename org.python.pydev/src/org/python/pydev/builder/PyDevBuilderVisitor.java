/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Fabio Zadrozny
 */
public abstract class PyDevBuilderVisitor implements IResourceDeltaVisitor {


	/**
	 * Visits the resource delta tree determining which files to rebuild (*.py).
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		int type = resource.getType();
		if (type == IResource.FILE) {
			if (resource.getFileExtension().equals("py")) {
				switch (delta.getKind()) {
					case IResourceDelta.ADDED :
					    visitResource(resource);
						break;
					case IResourceDelta.CHANGED:
					    visitResource(resource);
					case IResourceDelta.REMOVED:
						// Do nothing
						break;
				}
			}
		}
		return true;
	}

    /**
     * @param resource to be visited.
     */
    public abstract boolean visitResource(IResource resource);

	/**
	 * Default implementation. 
	 * Visits each resource once at a time.
	 * May be overriden if a better implementation is needed.
	 * 
	 * @param resourcesToParse list of resources from project that are python files.
	 */
    public void fullBuild(List resourcesToParse){
        for (Iterator iter = resourcesToParse.iterator(); iter.hasNext();) {
            visitResource((IResource) iter.next());
        }
    }

    /**
     * Checks pre-existance of marker.
     * 
     * @param resource resource in wich marker will searched
     * @param message message for marker
     * @param lineNumber line number where marker should exist
     * @return pre-existance of marker
     */
    public static IMarker markerExists(IResource resource, String message, int lineNumber, String type) {
        IMarker[] tasks;
        try {
            tasks = resource.findMarkers(type, true, IResource.DEPTH_ZERO);
            for (int i = 0; i < tasks.length; i++) {
                if (tasks[i].getAttribute(IMarker.LINE_NUMBER).toString().equals(String.valueOf(lineNumber))
                        && tasks[i].getAttribute(IMarker.MESSAGE).equals(message))
                    return tasks[i];
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Creates the marker for the problem.
     * 
     * @param resource resource for wich marker will be created
     * @param message message for marker
     * @param lineNumber line number of where marker will be tagged on to resource
     * @return
     */
    public static IMarker createProblemMarker(IResource resource, String message, int lineNumber) {
        String markerType = IMarker.PROBLEM;

        return createWarningMarker(resource, message, lineNumber, markerType);
    }

    /**
     * @param resource
     * @param message
     * @param lineNumber
     * @param markerType
     * @return
     */
    public static IMarker createWarningMarker(IResource resource, String message, int lineNumber, String markerType) {
    
        int severity = IMarker.SEVERITY_WARNING;
        return createMarker(resource, message, lineNumber, markerType, severity);
    }

    /**
     * @param resource
     * @param message
     * @param lineNumber
     * @param markerType
     * @param marker
     * @param severity
     * @return
     */
    public static IMarker createMarker(IResource resource, String message, int lineNumber, String markerType, int severity) {
        if(lineNumber <= 0)
            lineNumber = 1;
        IMarker marker = markerExists(resource, message, lineNumber, markerType);
        if (marker == null) {
            try {
                marker = resource.createMarker(markerType);
                marker.setAttribute(IMarker.MESSAGE, message);
                marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
                marker.setAttribute(IMarker.SEVERITY, severity);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
        return marker;
    }

    
}
