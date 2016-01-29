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

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        FastStringBuffer buf = new FastStringBuffer();

        if (textViewer instanceof PySourceViewer) {
            PySourceViewer s = (PySourceViewer) textViewer;
            getMarkerHover(hoverRegion, s, buf);
        }

        return buf.toString();
    }

    @Override
    public boolean isContentTypeSupported(String contentType) {
        boolean pythonCommentOrMultiline = false;

        for (String type : IPythonPartitions.types) {
            if (type.equals(contentType)) {
                pythonCommentOrMultiline = true;
                break;
            }
        }
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
