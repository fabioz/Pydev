/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.util.HashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * @author Fabio Zadrozny
 */
public abstract class PyDevBuilderVisitor implements IResourceDeltaVisitor {



	/**
	 * Visits the resource delta tree determining which files to rebuild (*.py).
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		if(delta == null){
		    return true;
		}

		IResource resource = delta.getResource();
		if(resource == null || resource.getFileExtension() == null){ //resource.getFileExtension() may return null if it has none.
		    return true;
		}
		
		int type = resource.getType();
		if (type == IResource.FILE) {
			if (resource.getFileExtension().equals("py")) {
				switch (delta.getKind()) {
					case IResourceDelta.ADDED :
					    visitResource(resource, PyDevBuilder.getDocFromResource(resource));
						break;
					case IResourceDelta.CHANGED:
					    visitResource(resource, PyDevBuilder.getDocFromResource(resource));
						break;
					case IResourceDelta.REMOVED:
					    visitRemovedResource(resource, PyDevBuilder.getDocFromResource(resource));
						break;
				}
			}
		}
		return true;
	}

    /**
     * @param resource to be visited.
     */
    public abstract boolean visitResource(IResource resource, IDocument document);

    /**
     * @param resource to be visited.
     */
    public abstract boolean visitRemovedResource(IResource resource, IDocument document);


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
    public static void createProblemMarker(IResource resource, String message, int lineNumber) {
        String markerType = IMarker.PROBLEM;

        createWarningMarker(resource, message, lineNumber, markerType);
    }

    /**
     * @param resource
     * @param message
     * @param lineNumber
     * @param markerType
     * @return
     */
    public static void createWarningMarker(IResource resource, String message, int lineNumber, String markerType) {
    
        int severity = IMarker.SEVERITY_WARNING;
        createMarker(resource, message, lineNumber, markerType, severity);
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
    public static void createMarker(IResource resource, String message, int lineNumber, String markerType, int severity) {
        if(lineNumber <= 0)
            lineNumber = 1;
        IMarker marker = markerExists(resource, message, lineNumber, markerType);
        if (marker == null) {
            try {
                HashMap map = new HashMap();
                map.put(IMarker.MESSAGE, message);
                map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
                map.put(IMarker.SEVERITY, new Integer(severity));

                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void createMarker(IResource resource, String message, int lineNumber, String markerType, int severity, boolean userEditable, boolean istransient) {
        if(lineNumber <= 0)
            lineNumber = 1;
        IMarker marker = markerExists(resource, message, lineNumber, markerType);
        if (marker == null) {
            try {
                HashMap map = new HashMap();
                map.put(IMarker.MESSAGE, message);
                map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
                map.put(IMarker.SEVERITY, new Integer(severity));
                map.put(IMarker.USER_EDITABLE, new Boolean(userEditable));
                map.put(IMarker.TRANSIENT, new Boolean(istransient));

                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
}
