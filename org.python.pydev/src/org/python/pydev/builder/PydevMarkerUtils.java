/*
 * Created on 11/09/2005
 */
package org.python.pydev.builder;

import java.util.HashMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.plugin.PydevPlugin;

public class PydevMarkerUtils {

    /**
     * Checks pre-existance of marker.
     */
    public static IMarker markerExists(IResource resource, String message, int charStart, int charEnd, String type) {
        IMarker[] tasks;
        try {
            tasks = resource.findMarkers(type, true, IResource.DEPTH_ZERO);
            for (int i = 0; i < tasks.length; i++) {
                IMarker task = tasks[i];
                
                boolean eqMessage = task.getAttribute(IMarker.MESSAGE).equals(message);
                
                boolean eqCharStart = (Integer)task.getAttribute(IMarker.CHAR_START) == charStart;
                boolean eqCharEnd = (Integer)task.getAttribute(IMarker.CHAR_END) == charEnd;
                
                if (eqMessage && eqCharStart && eqCharEnd){
                    return task;
                }
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        return null;
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
                IMarker task = tasks[i];
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
    
        IMarker marker = markerExists(resource, message, startAbsolute, endAbsolute, markerType);
        if (marker == null) {
            try {
                
                
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(IMarker.MESSAGE, message);
                map.put(IMarker.LINE_NUMBER, new Integer(lineStart));
                map.put(IMarker.CHAR_START, new Integer(startAbsolute));
                map.put(IMarker.CHAR_END, new Integer(endAbsolute));
                map.put(IMarker.SEVERITY, new Integer(severity));
                
                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
    }

    public static void createMarker(IResource resource, String message, int lineNumber, String markerType, int severity) {
        if(lineNumber <= 0)
            lineNumber = 0;
        IMarker marker = markerExists(resource, message, lineNumber, markerType);
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
        }
    }

    public static void createMarker(IResource resource, String message, int lineNumber, String markerType, int severity, boolean userEditable, boolean istransient) {
        if(lineNumber <= 0)
            lineNumber = 0;
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
        }
    }

}
