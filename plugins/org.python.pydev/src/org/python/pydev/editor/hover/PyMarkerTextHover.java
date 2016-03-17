/**
 * Copyright (c) 2016 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 * 
 * A re-factor of <code>PyTextHover</code> to use the extension point <code>org.python.pydev.pyTextHover</code>
 */
package org.python.pydev.editor.hover;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.editor.PyInformationPresenter;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class PyMarkerTextHover extends AbstractPyEditorTextHover {

    public static String ID = "org.python.pydev.editor.hover.pyMarkerTextHover";

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
     */
    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        FastStringBuffer buf = new FastStringBuffer();

        if (textViewer instanceof PySourceViewer) {
            PySourceViewer s = (PySourceViewer) textViewer;
            getMarkerHover(hoverRegion, s, buf);
        }

        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * @see org.python.pydev.editor.hover.AbstractPyEditorTextHover#isContentTypeSupported(java.lang.String)
     */
    @Override
    public boolean isContentTypeSupported(String contentType) {
        boolean pythonCommentOrMultiline = IPythonPartitions.NON_DEFAULT_TYPES_AS_SET.contains(contentType);
        return !pythonCommentOrMultiline;
    }

    /**
     * Fills the buffer with the text for markers we're hovering over.
     */
    private void getMarkerHover(IRegion hoverRegion, PySourceViewer s, FastStringBuffer buf) {
        for (Iterator<MarkerAnnotationAndPosition> it = s.getMarkerIterator(); it.hasNext();) {
            MarkerAnnotationAndPosition marker = it.next();
            try {
                if (marker.position == null) {
                    continue;
                }
                int cStart = marker.position.offset;
                int cEnd = cStart + marker.position.length;
                int offset = hoverRegion.getOffset();
                if (cStart <= offset && cEnd >= offset) {
                    if (buf.length() > 0) {
                        buf.append(PyInformationPresenter.LINE_DELIM);
                    }
                    Object msg = marker.markerAnnotation.getMarker().getAttribute(IMarker.MESSAGE);
                    if (!"PyDev breakpoint".equals(msg)) {
                        buf.appendObject(msg);
                    }
                }
            } catch (CoreException e) {
                //ignore marker does not exist anymore
            }
        }
    }

}
