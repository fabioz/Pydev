package org.python.pydev.editor.codefolding;

import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.MarkerAnnotation;


/**
 * This class bundles the marker annotation and a related position.
 */
public class MarkerAnnotationAndPosition{

    public final MarkerAnnotation markerAnnotation;
    public final Position position;

    public MarkerAnnotationAndPosition(MarkerAnnotation markerAnnotation, Position position) {
        this.markerAnnotation = markerAnnotation;
        this.position = position;
    }

}
