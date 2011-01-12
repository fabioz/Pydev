package org.python.pydev.editor.codefolding;

import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;


/**
 * This class bundles the marker annotation and a related position.
 */
public class MarkerAnnotationAndPosition{

    public final SimpleMarkerAnnotation markerAnnotation;
    public final Position position;

    public MarkerAnnotationAndPosition(SimpleMarkerAnnotation markerAnnotation, Position position) {
        this.markerAnnotation = markerAnnotation;
        this.position = position;
    }

}
