package org.python.pydev.editor.correctionassist;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

/**
 * This class can later be used to make quick fixes. 
 * 
 * 
 * @author Fabio Zadrozny
 */
public class PyQuickFix implements IMarkerResolutionGenerator {

    public static String CONTENT_GENERATOR_MARKER = "org.python.pydev.editor.markers.pycontentgeneratormarker"; 

    public IMarkerResolution[] getResolutions(IMarker mk) {
        try {
           Object problem = mk.getAttribute("WhatsUp");
           return new IMarkerResolution[] {
//              new MarkerResolution("Fix #1 for "+problem),
//              new MarkerResolution("Fix #2 for "+problem),
           };
        }
        catch (CoreException e) {
           return new IMarkerResolution[0];
        }
     }

}
