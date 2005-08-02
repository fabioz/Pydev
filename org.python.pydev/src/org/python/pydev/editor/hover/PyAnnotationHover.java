/*
 * License: Common Public License v1.0
 * Created on 02/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.hover;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.plugin.PydevPlugin;

public class PyAnnotationHover implements IAnnotationHover{

    public PyAnnotationHover(ISourceViewer sourceViewer) {
    }

    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
        StringBuffer buf = new StringBuffer();
        
        if(sourceViewer instanceof PySourceViewer){
            PySourceViewer s = (PySourceViewer) sourceViewer;
            for(IMarker marker : s.getMarkerIteratable()){
                try {
                    Integer line = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
                    if(line != null){
                        if(line == lineNumber){
                            if(buf.length() >0){
                                buf.append("\n");
                            }
                            buf.append(marker.getAttribute(IMarker.MESSAGE));
                        }
                    }
                } catch (CoreException e) {
                    PydevPlugin.log(e);
                }
            }
            
        }
        return buf.toString();
    }

}
