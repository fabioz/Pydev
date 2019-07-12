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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * Helper class to deal with markers.
 *
 * It's main use is to replace the markers in a given resource for another set of markers.
 *
 * @author Fabio
 */
public class PyMarkerUIUtils {

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
        IResource resource = input.getAdapter(IFile.class);
        if (resource == null) {
            resource = input.getAdapter(IResource.class);
        }
        if (resource == null) {
            resource = ResourcesPlugin.getWorkspace().getRoot();
        }
        return resource;
    }

}
