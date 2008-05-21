/*
 * Created on 11/09/2005
 */
package org.python.pydev.builder;

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

/**
 * Helper class to deal with markers.
 * 
 * It's main use is to replace the markers in a given resource for another set of markers.
 *
 * @author Fabio
 */
public class PydevMarkerUtils {
    
    /**
     * This class represents the information to create a marker.
     *
     * @author Fabio
     */
    public static class MarkerInfo{
        public IDocument doc;
        public String message; 
        public String markerType;
        public int severity; 
        public boolean userEditable; 
        public boolean isTransient;
        public int lineStart;
        public int colStart;
        public int lineEnd; 
        public int absoluteStart=-1;
        public int absoluteEnd=-1; 
        public int colEnd; 
        public Map<String, Object> additionalInfo;
        
        
        /**
         * Constructor passing lines and relative positions
         */
        public MarkerInfo(IDocument doc, String message, String markerType, int severity, boolean userEditable,
                boolean isTransient, int lineStart, int colStart, int lineEnd, int colEnd,
                Map<String, Object> additionalInfo) {
            super();
            this.doc = doc;
            this.message = message;
            this.markerType = markerType;
            this.severity = severity;
            this.userEditable = userEditable;
            this.isTransient = isTransient;
            this.lineStart = lineStart;
            this.colStart = colStart;
            this.lineEnd = lineEnd;
            this.colEnd = colEnd;
            this.additionalInfo = additionalInfo;
        }
        
        
        /**
         * Constructor passing absolute position
         */
        public MarkerInfo(IDocument doc, String message, String markerType, int severity, boolean userEditable,
                boolean isTransient, int line, int absoluteStart, int absoluteEnd,
                Map<String, Object> additionalInfo) {
            super();
            this.doc = doc;
            this.message = message;
            this.markerType = markerType;
            this.severity = severity;
            this.userEditable = userEditable;
            this.isTransient = isTransient;
            this.lineStart = line;
            this.lineEnd = line;
            this.absoluteStart = absoluteStart;
            this.absoluteEnd = absoluteEnd;
            this.additionalInfo = additionalInfo;
        }

        /**
         * @return a map with the properties to be set in the marker
         * @throws BadLocationException 
         */
        private HashMap<String, Object> getAsMap() throws BadLocationException {
            
            if (lineStart < 0) {
                lineStart = 0;
            }

            
            if(absoluteStart == -1 || absoluteEnd == -1){
                //if the absolute wasn't specified, let's calculate it
                IRegion start = doc.getLineInformation(lineStart);
                absoluteStart = start.getOffset() + colStart;
                if (lineEnd >= 0 && colEnd >= 0) {
                    IRegion end = doc.getLineInformation(lineEnd);
                    absoluteEnd = end.getOffset() + colEnd;
                } else {
                    //ok, we have to calculate it based on the line contents...
                    String line = doc.get(start.getOffset(), start.getLength());
                    int i;
                    StringBuffer buffer;
                    if ((i = line.indexOf('#')) != -1) {
                        buffer = new StringBuffer(line.substring(0, i));
                    } else {
                        buffer = new StringBuffer(line);
                    }
                    while (buffer.length() > 0 && Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {
                        buffer.deleteCharAt(buffer.length() - 1);
                    }
                    absoluteEnd = start.getOffset() + buffer.length();
                }
            }
            
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put(IMarker.MESSAGE, message);
            map.put(IMarker.LINE_NUMBER, lineStart);
            map.put(IMarker.CHAR_START, absoluteStart);
            map.put(IMarker.CHAR_END, absoluteEnd);
            map.put(IMarker.SEVERITY, severity);
            map.put(IMarker.USER_EDITABLE, userEditable);
            map.put(IMarker.TRANSIENT, isTransient);
            
            if(additionalInfo != null){
                map.putAll(additionalInfo);
            }
            return map;
        }
        
        
    }



    /**
     * This method allows clients to rplace the existing markers of some type in a given resource for other markers.
     * 
     * @param lst the new markers to be set in the resource
     * @param resource the resource were the markers should be replaced
     * @param markerType the type of the marker that'll be replaced
     */
    public static void replaceMarkers(List<MarkerInfo> lst, IResource resource, String markerType) {
        IMarker[] existingMarkers;
        try {
            existingMarkers = resource.findMarkers(markerType, false, IResource.DEPTH_ZERO);
        } catch (CoreException e1) {
            PydevPlugin.log(e1);
            existingMarkers = new IMarker[0];
        }
        
        int lastExistingUsed = 0;
        try {
            for (MarkerInfo markerInfo : lst) {
                if(lastExistingUsed < existingMarkers.length){
                    IMarker marker = existingMarkers[lastExistingUsed];
                    marker.setAttributes(markerInfo.getAsMap());
                    lastExistingUsed += 1;
                }else{
                    MarkerUtilities.createMarker(resource, markerInfo.getAsMap(), markerType);
                }
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        
        //erase the ones that weren't replaced.
        try {
            for(int i=lastExistingUsed; i < existingMarkers.length; i++){
                //erase the ones we didn't use
                existingMarkers[i].delete();
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
    }
}
