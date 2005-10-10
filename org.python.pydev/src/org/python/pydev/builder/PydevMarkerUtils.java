/*
 * Created on 11/09/2005
 */
package org.python.pydev.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.plugin.PydevPlugin;

public class PydevMarkerUtils {

    public static IMarker markerExists(IResource resource, String message, int charStart, int charEnd, String type) {
        return markerExists(resource, message, charStart, charEnd, type, null);
    }
    /**
     * Checks pre-existance of marker.
     */
    public static IMarker markerExists(IResource resource, String message, int charStart, int charEnd, String type, List<IMarker> existingMarkers) {
        existingMarkers = checkExistingMarkers(resource, type, existingMarkers);
        
        try {
            for (IMarker task : existingMarkers) {
                Object msg = task.getAttribute(IMarker.MESSAGE);
                Object start = task.getAttribute(IMarker.CHAR_START);
                Object end = task.getAttribute(IMarker.CHAR_END);


                if(msg == null || start == null || end == null || message == null){
                	return null;
                }
                boolean eqMessage = msg.equals(message);
                boolean eqCharStart = (Integer) start == charStart;
				boolean eqCharEnd = (Integer) end == charEnd;

                if (eqMessage && eqCharStart && eqCharEnd) {
                    return task;
                }
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        return null;
    }

    public static IMarker markerExists(IResource resource, String message, int lineNumber, String type) {
        return markerExists(resource, message, lineNumber, lineNumber, type, null);
    }
    /**
     * Checks pre-existance of marker.
     * 
     * @param resource resource in wich marker will searched
     * @param message message for marker
     * @param lineNumber line number where marker should exist
     * @return pre-existance of marker
     */
    public static IMarker markerExists(IResource resource, String message, int lineNumber, String type, List<IMarker> existingMarkers) {
        existingMarkers = checkExistingMarkers(resource, type, existingMarkers);
        
        try {
            for (IMarker task : existingMarkers) {
                boolean eqLineNumber = (Integer)task.getAttribute(IMarker.LINE_NUMBER) == lineNumber;
                boolean eqMessage = task.getAttribute(IMarker.MESSAGE).equals(message);
                if (eqLineNumber && eqMessage){
                    return task;
                }
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
    
        PydevMarkerUtils.createWarningMarker(resource, message, lineNumber, markerType);
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
        PydevMarkerUtils.createMarker(resource, message, lineNumber, markerType, severity);
    }

    public static void createMarker(IResource resource, IDocument doc, String message, 
            int lineStart, int colStart, int lineEnd, int colEnd, 
            String markerType, int severity) {
        createMarker(resource, doc, message, lineStart, colStart, lineEnd, colEnd, markerType, severity, null);
    }

    public static IMarker createMarker(IResource resource, IDocument doc, String message, 
            int lineStart, int colStart, int lineEnd, int colEnd, 
            String markerType, int severity, Map<String, Object> additionalInfo) {
        return createMarker(resource, doc, message, lineStart, colStart, lineEnd, colEnd, markerType, severity, additionalInfo, null);
    }
    
    public static IMarker createMarker(IResource resource, IDocument doc, String message, 
            int lineStart, int colStart, int lineEnd, int colEnd, 
            String markerType, int severity, Map<String, Object> additionalInfo, List<IMarker> existingMarkers) {
    
        existingMarkers = checkExistingMarkers(resource, markerType, existingMarkers);

        if(lineStart < 0){
            lineStart = 0;
        }
        
        int startAbsolute;
        int endAbsolute;
        
        try {
            IRegion start = doc.getLineInformation(lineStart);
            startAbsolute = start.getOffset() + colStart;
            if (lineEnd >= 0 && colEnd >= 0) {
                IRegion end = doc.getLineInformation(lineEnd);
                endAbsolute = end.getOffset() + colEnd;
            } else {
                //ok, we have to calculate it based on the line contents...
                String line = doc.get(start.getOffset(), start.getLength());
                int i;
                StringBuffer buffer;
                if((i = line.indexOf('#')) != -1){
                    buffer = new StringBuffer(line.substring(0, i));
                }else{
                    buffer = new StringBuffer(line);
                }
                while(buffer.length() > 0 && Character.isWhitespace(buffer.charAt(buffer.length() - 1))){
                    buffer.deleteCharAt(buffer.length() -1);
                }
                endAbsolute = start.getOffset() + buffer.length();
            }
        } catch (BadLocationException e) {
            throw new RuntimeException("Unable to get the location requested for the resource "+resource.getLocation(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    
        IMarker marker = markerExists(resource, message, startAbsolute, endAbsolute, markerType, existingMarkers);
        if (marker == null) {
            try {
                
                
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(IMarker.MESSAGE, message);
                map.put(IMarker.LINE_NUMBER, new Integer(lineStart));
                map.put(IMarker.CHAR_START, new Integer(startAbsolute));
                map.put(IMarker.CHAR_END, new Integer(endAbsolute));
                map.put(IMarker.SEVERITY, new Integer(severity));
                
                //add the additional info
                for (Map.Entry<String, Object> entry : additionalInfo.entrySet()) {
                    map.put(entry.getKey(), entry.getValue());
                }
                
                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }else{
        	//to check if it exists, we don't check all attributes, so, let's update those that we don't check.
        	try {
				marker.setAttribute(IMarker.LINE_NUMBER, new Integer(lineStart));
				marker.setAttribute(IMarker.SEVERITY, new Integer(severity));
			} catch (Exception e) {
				PydevPlugin.log(e);
			}
            existingMarkers.remove(marker);
        }
        return marker;
    }
    /**
     * @param resource
     * @param markerType
     * @param existingMarkers
     * @return
     */
    private static List<IMarker> checkExistingMarkers(IResource resource, String markerType, List<IMarker> existingMarkers) {
        if(existingMarkers == null){
            try {
                existingMarkers = new ArrayList<IMarker>();
                IMarker[] markers = resource.findMarkers(markerType, true, IResource.DEPTH_ZERO);
                for (IMarker marker : markers) {
                    existingMarkers.add(marker);
                }
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
        return existingMarkers;
    }
    
    public static IMarker createMarker(IResource resource, String message, int lineNumber, String markerType, int severity) {
    	if(message == null){
    		throw new RuntimeException("The marker message may not be null.");
    	}
        return createMarker(resource, message, lineNumber, markerType, severity, null);
    }

    public static IMarker createMarker(IResource resource, String message, int lineNumber, String markerType, int severity, List<IMarker> existingMarkers) {
        if(lineNumber <= 0){
            lineNumber = 0;
        }
        existingMarkers = checkExistingMarkers(resource, markerType, existingMarkers);
        IMarker marker = markerExists(resource, message, lineNumber, markerType, existingMarkers);
        if (marker == null) {
            try {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(IMarker.MESSAGE, message);
                map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
                map.put(IMarker.SEVERITY, new Integer(severity));
    
                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }else{
            existingMarkers.remove(marker);
        }
        return marker;
    }

    public static IMarker createMarker(IResource resource, String message, int lineNumber, String markerType, int severity, boolean userEditable, boolean istransient) {
        return createMarker(resource, message, lineNumber, markerType, severity, userEditable, istransient, null);
    }
    
    public static IMarker createMarker(IResource resource, String message, int lineNumber, String markerType, int severity, boolean userEditable, boolean istransient, List<IMarker> existingMarkers) {
        if(lineNumber <= 0){
            lineNumber = 0;
        }
        
        existingMarkers = checkExistingMarkers(resource, markerType, existingMarkers);
        IMarker marker = markerExists(resource, message, lineNumber, markerType);

        if (marker == null) {
            try {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(IMarker.MESSAGE, message);
                map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
                map.put(IMarker.SEVERITY, new Integer(severity));
                map.put(IMarker.USER_EDITABLE, new Boolean(userEditable));
                map.put(IMarker.TRANSIENT, new Boolean(istransient));
    
                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }else{
            existingMarkers.remove(marker);
        }
        return marker;
    }

}
