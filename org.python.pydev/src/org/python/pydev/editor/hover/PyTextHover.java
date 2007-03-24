/*
 * License: Common Public License v1.0
 * Created on 02/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.hover;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.plugin.PydevPlugin;

public class PyTextHover implements ITextHover{

    private boolean pythonCommentOrMultiline;

    public PyTextHover(ISourceViewer sourceViewer, String contentType) {
        pythonCommentOrMultiline = false;
        
        for(String type : IPythonPartitions.types){
            if(type.equals(contentType)){
                pythonCommentOrMultiline = true;
            }
        }
    }

    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        StringBuffer buf = new StringBuffer();
        if(!pythonCommentOrMultiline){
            if(textViewer instanceof PySourceViewer){
                PySourceViewer s = (PySourceViewer) textViewer;
                
                for(IMarker marker : s.getMarkerIteratable()){
                    try {
                        Integer cStart = (Integer) marker.getAttribute(IMarker.CHAR_START);
                        Integer cEnd = (Integer) marker.getAttribute(IMarker.CHAR_END);
                        if(cStart != null && cEnd != null){
                            int offset = hoverRegion.getOffset();
                            if(cStart <= offset && cEnd >= offset){
                                if(buf.length() >0){
                                    buf.append("\n");
                                }
                                buf.append(marker.getAttribute(IMarker.MESSAGE));
                            }
                        }
                    } catch (CoreException e) {
                        //ignore marker does not exist anymore
                    }
                }

            }
        }
        return buf.toString();
    }

    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        return new Region(offset, 0);
    }

}
