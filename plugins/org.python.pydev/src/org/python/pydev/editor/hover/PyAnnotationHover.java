/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 02/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.hover;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class PyAnnotationHover implements IAnnotationHover {

    public PyAnnotationHover(ISourceViewer sourceViewer) {
    }

    @Override
    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
        FastStringBuffer buf = new FastStringBuffer();

        if (sourceViewer instanceof PySourceViewer) {
            PySourceViewer s = (PySourceViewer) sourceViewer;

            for (MarkerAnnotationAndPosition marker : s.getMarkersAtLine(lineNumber, null)) {
                try {
                    if (buf.length() > 0) {
                        buf.append("\n");
                    }
                    buf.appendObject(marker.markerAnnotation.getMarker().getAttribute(IMarker.MESSAGE));
                } catch (CoreException e) {
                    Log.log(e);
                }
            }

        }
        return buf.toString();
    }

}
