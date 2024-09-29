/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codefolding;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;
import org.python.pydev.core.IMarkerInfoForAnalysis;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.IMiscConstants;

import com.python.pydev.analysis.additionalinfo.builders.AnalysisRunner;

/**
 * This class bundles the marker annotation and a related position.
 */
public class MarkerAnnotationAndPosition {

    public IMarkerInfoForAnalysis asMarkerInfoForAnalysis() {
        return new IMarkerInfoForAnalysis() {

            @Override
            public Object getPyLintMessageIdAttribute() {
                return getAttribute(IMiscConstants.PYLINT_MESSAGE_ID);
            }

            private Object getAttribute(String attr) {
                IMarker marker = markerAnnotation.getMarker();
                Object attribute;
                try {
                    attribute = marker.getAttribute(attr);
                } catch (Exception e) {
                    Log.log(e);
                    return null;
                }
                return attribute;
            }

            @Override
            public Integer getPyDevAnalisysType() {
                IMarker marker = markerAnnotation.getMarker();
                Integer id;
                try {
                    id = (Integer) marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
                } catch (Exception e) {
                    Log.log(e);
                    return null;
                }
                return id;
            }

            @Override
            public Object getFlake8MessageId() {
                return getAttribute(IMiscConstants.FLAKE8_MESSAGE_ID);
            }

            @Override
            public Object getMessage() {
                return getAttribute(IMarker.MESSAGE);
            }

            @Override
            public boolean hasPosition() {
                return position != null;
            }

            @Override
            public int getOffset() {
                return position.offset;
            }

            @Override
            public int getLength() {
                return position.length;
            }

            @Override
            public void delete() {
                IMarker marker = markerAnnotation.getMarker();
                if (marker != null) {
                    try {
                        marker.delete();
                    } catch (CoreException e) {
                        Log.log(e);
                    }
                }
            }
        };
    }

    public final SimpleMarkerAnnotation markerAnnotation;
    /**
     * May be null!
     */
    public final Position position;

    public MarkerAnnotationAndPosition(SimpleMarkerAnnotation markerAnnotation, Position position) {
        this.markerAnnotation = markerAnnotation;
        this.position = position;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((markerAnnotation == null) ? 0 : markerAnnotation.getText().hashCode());
        return result;
    }

    /**
     * Note that the equals and hashCode only work in the marker annotation, not in the position (because we want
     * to make unique based on the marker and not on its position when analyzing a line).
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MarkerAnnotationAndPosition)) {
            return false;
        }

        MarkerAnnotationAndPosition other = (MarkerAnnotationAndPosition) obj;

        if (markerAnnotation == null) {
            if (other.markerAnnotation != null) {
                return false;
            }
            //if here, markerAnnotation == other.markerAnnotation == null
            return true;
        } else if (other.markerAnnotation == null) {
            return false;
        }

        String text = this.markerAnnotation.getText();
        String otherText = other.markerAnnotation.getText();
        if (text == null) {
            if (otherText != null) {
                return false;
            }
            //if here, text == otherText == null
        } else if (!text.equals(otherText)) {
            return false;
        }
        return this.markerAnnotation.getType().equals(other.markerAnnotation.getType());
    }
}
