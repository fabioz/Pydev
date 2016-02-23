/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist;

import org.eclipse.core.resources.IMarker;
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

    @Override
    public IMarkerResolution[] getResolutions(IMarker mk) {
        return new IMarkerResolution[] {};
        //        try {
        //           Object problem = mk.getAttribute("WhatsUp");
        //           return new IMarkerResolution[] {
        //              new MarkerResolution("Fix #1 for "+problem),
        //              new MarkerResolution("Fix #2 for "+problem),
        //           };
        //        }
        //        catch (CoreException e) {
        //           return new IMarkerResolution[0];
        //        }
    }

}
