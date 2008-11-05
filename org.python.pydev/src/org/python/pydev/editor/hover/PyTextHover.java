/*
 * License: Common Public License v1.0
 * Created on 02/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.hover;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PyInformationPresenter;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class PyTextHover implements ITextHover, ITextHoverExtension{

    private boolean pythonCommentOrMultiline;
    private FastStringBuffer buf = new FastStringBuffer();

    public PyTextHover(ISourceViewer sourceViewer, String contentType) {
        pythonCommentOrMultiline = false;
        
        for(String type : IPythonPartitions.types){
            if(type.equals(contentType)){
                pythonCommentOrMultiline = true;
            }
        }
    }

    /**
     * Synchronized because of buffer access.
     */
    public synchronized String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        buf.clear();
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
                                buf.appendObject(marker.getAttribute(IMarker.MESSAGE));
                            }
                        }
                    } catch (CoreException e) {
                        //ignore marker does not exist anymore
                    }
                }
                
                
                //Now, aside from the marker, let's check if there's some definition we should show the user about.
                CompletionCache completionCache = new CompletionCache();
                ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
                
                PyEdit edit = s.getEdit();
                PySelection ps = new PySelection(s.getDocument(), hoverRegion.getOffset()+hoverRegion.getLength());
                RefactoringRequest request = new RefactoringRequest(edit.getEditorFile(), ps, new NullProgressMonitor(), edit.getPythonNature(), edit);
                String[] tokenAndQual = PyRefactoringFindDefinition.findActualDefinition(request, completionCache, selected);
                
                
                if(tokenAndQual != null && selected.size() > 0){
                    for (IDefinition d : selected) {
                        Definition def = (Definition) d;
                        String docstring = d.getDocstring();
                        
                        if(buf.length() > 0 && (docstring != null || def.value != null || def.module != null)){
                            buf.append(PyInformationPresenter.LINE_DELIM);
                        }
                        
                        
                        if(def.value != null){
                            buf.append("<pydev_hint_bold>");
                            buf.append(def.value);
                            buf.append(' ');
                            buf.append("</pydev_hint_bold>");
                        }
                        
                        if(def.module != null){
                            buf.append("(");
                            buf.append("<pydev_hint_bold>");
                            buf.append(def.module.getName());
                            buf.append("</pydev_hint_bold>");
                            buf.append(")");
                            buf.append(PyInformationPresenter.LINE_DELIM);
                        }
                        
                        if(docstring != null && docstring.trim().length() > 0){
                            buf.append(StringUtils.removeWhitespaceColumnsToLeft(docstring));
                        }
                    }
                }

            }
        }
        return buf.toString();
    }

    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        return new Region(offset, 0);
    }

    /*
     * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
     */
    public IInformationControlCreator getHoverControlCreator() {
        return new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, SWT.NONE, new PyInformationPresenter(), EditorsUI.getTooltipAffordanceString());
            }
        };
    }
}
