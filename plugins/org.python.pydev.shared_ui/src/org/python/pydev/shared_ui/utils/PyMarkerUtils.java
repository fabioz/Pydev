/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 11/09/2005
 */
package org.python.pydev.shared_ui.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.utils.ArrayUtils;

/**
 * Helper class to deal with markers.
 *
 * It's main use is to replace the markers in a given resource for another set of markers.
 *
 * @author Fabio
 */
public class PyMarkerUtils {

    /**
     * This class represents the information to create a marker.
     *
     * @author Fabio
     */
    public static class MarkerInfo {
        public IDocument doc;
        public String message;
        public String markerType;
        public int severity;
        public boolean userEditable;
        public boolean isTransient;
        public int lineStart;
        public int colStart;
        public int lineEnd;
        public int absoluteStart = -1;
        public int absoluteEnd = -1;
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
                boolean isTransient, int line, int absoluteStart, int absoluteEnd, Map<String, Object> additionalInfo) {
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
         * @return a map with the properties to be set in the marker or null if some error happened while doing it.
         * @throws BadLocationException
         */
        private HashMap<String, Object> getAsMap() {

            if (lineStart < 0) {
                lineStart = 0;
            }

            if (absoluteStart == -1 || absoluteEnd == -1) {
                //if the absolute wasn't specified, let's calculate it
                IRegion start;
                try {
                    start = doc.getLineInformation(lineStart);
                } catch (BadLocationException e) {
                    //Don't log it. Just return null -- this happens because there's a delay from calculating things
                    //to actually using them and the document might have changed and the given line is no longer available
                    //(a new request should fix this)
                    return null;
                } catch (Exception e) {
                    Log.log(IStatus.ERROR, "Could not get line: " + lineStart + " to add message: " + message, e);
                    return null;
                }

                try {
                    absoluteStart = start.getOffset() + colStart;
                    if (lineEnd >= 0 && colEnd >= 0) {
                        IRegion end = doc.getLineInformation(lineEnd);
                        absoluteEnd = end.getOffset() + colEnd;
                    } else {
                        //ok, we have to calculate it based on the line contents...
                        String line = doc.get(start.getOffset(), start.getLength());
                        int i;
                        FastStringBuffer buffer;
                        if ((i = line.indexOf('#')) != -1) {
                            buffer = new FastStringBuffer(line.substring(0, i), 0);
                        } else {
                            buffer = new FastStringBuffer(line, 0);
                        }
                        while (buffer.length() > 0 && Character.isWhitespace(buffer.lastChar())) {
                            buffer.deleteLast();
                        }
                        absoluteEnd = start.getOffset() + buffer.length();
                    }
                } catch (BadLocationException e) {
                    //Don't log it. Just return null -- this happens because there's a delay from calculating things
                    //to actually using them and the document might have changed and the given line is no longer available
                    //(a new request should fix this and create the markers correctly because of the change in the document)
                    return null;

                } catch (Exception e) {
                    Log.log(IStatus.INFO, "Problem creating map for:" + this.toString(), e);
                    return null;
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

            if (additionalInfo != null) {
                map.putAll(additionalInfo);
            }
            return map;
        }

        /**
         * Constructs a <code>String</code> with all attributes
         * in name = value format.
         *
         * @return a <code>String</code> representation
         * of this object.
         */
        @Override
        public String toString() {
            final String NL = "\n";

            StringBuffer retValue = new StringBuffer();

            retValue.append("MarkerInfo (\n").append("doc = ").append(this.doc).append(NL).append("message = ")
                    .append(this.message).append(NL).append("markerType = ").append(this.markerType).append(NL)
                    .append("severity = ").append(this.severity).append(NL).append("userEditable = ")
                    .append(this.userEditable).append(NL).append("isTransient = ").append(this.isTransient).append(NL)
                    .append("lineStart = ").append(this.lineStart).append(NL).append("colStart = ")
                    .append(this.colStart).append(NL).append("lineEnd = ").append(this.lineEnd).append(NL)
                    .append("absoluteStart = ").append(this.absoluteStart).append(NL).append("absoluteEnd = ")
                    .append(this.absoluteEnd).append(NL).append("colEnd = ").append(this.colEnd).append(NL)
                    .append("additionalInfo = ").append(this.additionalInfo).append(NL).append(")");

            return retValue.toString();
        }

    }

    /**
     * This method allows clients to replace the existing markers of some type in a given resource for other markers.
     *
     * @param lst the new markers to be set in the resource
     * @param resource the resource were the markers should be replaced
     * @param markerType the type of the marker that'll be replaced
     * @param removeUserEditable if true, will remove the user-editable markers too (otherwise, will leave the user-editable markers)
     * @param monitor used to check whether this process should be canceled.
     */
    @SuppressWarnings("unchecked")
    public static void replaceMarkers(final List<MarkerInfo> lst, final IResource resource, final String markerType,
            final boolean removeUserEditable, IProgressMonitor monitor) {
        List<Map<String, Object>> lMap = new ArrayList<Map<String, Object>>(lst.size());
        for (MarkerInfo markerInfo : lst) {
            try {
                HashMap<String, Object> asMap = markerInfo.getAsMap();
                if (asMap != null) {
                    lMap.add(asMap);
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        replaceMarkers(lMap.toArray(new Map[lMap.size()]), resource, markerType,
                removeUserEditable, monitor);
    }

    /**
     * This method allows clients to replace the existing markers of some type in a given resource for other markers.
     *
     * @param lst the new markers to be set in the resource
     * @param resource the resource were the markers should be replaced
     * @param markerType the type of the marker that'll be replaced
     * @param removeUserEditable if true, will remove the user-editable markers too (otherwise, will leave the user-editable markers)
     * @param monitor used to check whether this process should be canceled.
     */
    public static void replaceMarkers(final Map<String, Object>[] lst, final IResource resource,
            final String markerType,
            final boolean removeUserEditable, IProgressMonitor monitor) {
        IWorkspaceRunnable r = new IWorkspaceRunnable() {

            public void run(IProgressMonitor monitor) throws CoreException {
                if (!resource.exists()) {
                    return;
                }
                try {
                    if (removeUserEditable) {
                        resource.deleteMarkers(markerType, true, IResource.DEPTH_ZERO);

                    } else {
                        IMarker[] existingMarkers;
                        existingMarkers = resource.findMarkers(markerType, false, IResource.DEPTH_ZERO);
                        //we don't want to remove the user-editable markers, so, let's filter them out!
                        existingMarkers = ArrayUtils.filter(existingMarkers, new ICallback<Boolean, IMarker>() {

                            public Boolean call(IMarker marker) {
                                //if it's user-editable, it should not be included in the list
                                return !marker.getAttribute(IMarker.USER_EDITABLE, true); //default for user-editable is true.
                            }
                        }).toArray(new IMarker[0]);
                        ResourcesPlugin.getWorkspace().deleteMarkers(existingMarkers);
                    }
                } catch (Exception e1) {
                    Log.log(e1);
                }

                try {
                    for (Map<String, Object> asMap : lst) {
                        IMarker marker = resource.createMarker(markerType);
                        marker.setAttributes(asMap);
                    }
                } catch (Exception e) {
                    Log.log(e);
                }

            }
        };
        try {
            resource.getWorkspace().run(r, ResourcesPlugin.getWorkspace().getRuleFactory().markerRule(resource),
                    IWorkspace.AVOID_UPDATE, monitor);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * @param original
     * @param pydevCoverageMarker
     */
    public static void removeMarkers(IResource resource, String markerType) {
        if (resource == null) {
            return;
        }
        try {
            resource.deleteMarkers(markerType, false, IResource.DEPTH_ZERO);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * @return the position for a marker.
     */
    public static Position getMarkerPosition(IDocument document, IMarker marker, IAnnotationModel model) {
        if (model instanceof AbstractMarkerAnnotationModel) {
            Position ret = ((AbstractMarkerAnnotationModel) model).getMarkerPosition(marker);
            if (ret != null) {
                return ret;
            }
        }
        int start = MarkerUtilities.getCharStart(marker);
        int end = MarkerUtilities.getCharEnd(marker);

        if (start > end) {
            end = start + end;
            start = end - start;
            end = end - start;
        }

        if (start == -1 && end == -1) {
            // marker line number is 1-based
            int line = MarkerUtilities.getLineNumber(marker);
            if (line > 0 && document != null) {
                try {
                    start = document.getLineOffset(line - 1);
                    end = start;
                } catch (BadLocationException x) {
                }
            }
        }

        if (start > -1 && end > -1) {
            return new Position(start, end - start);
        }

        return null;
    }

    /**
     * @return the resource for which to create the marker or <code>null</code>
     *
     * If the editor maps to a workspace file, it will return that file. Otherwise, it will return the
     * workspace root (so, markers from external files will be created in the workspace root).
     */
    public static IResource getResourceForTextEditor(ITextEditor textEditor) {
        IEditorInput input = textEditor.getEditorInput();
        IResource resource = (IResource) input.getAdapter(IFile.class);
        if (resource == null) {
            resource = (IResource) input.getAdapter(IResource.class);
        }
        if (resource == null) {
            resource = ResourcesPlugin.getWorkspace().getRoot();
        }
        return resource;
    }
}
