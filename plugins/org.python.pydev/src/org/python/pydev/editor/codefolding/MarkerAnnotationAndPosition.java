/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codefolding;

import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

/**
 * This class bundles the marker annotation and a related position.
 */
public class MarkerAnnotationAndPosition {

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
