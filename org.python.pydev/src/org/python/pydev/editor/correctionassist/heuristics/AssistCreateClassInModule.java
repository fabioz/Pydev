/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.CompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.SourceModuleProposal;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistCreateClassInModule extends AbstractAssistCreate {

    private String getDeclToCreate(PySelection ps, int offset){
        try {
            int len = ps.getStartLine().getOffset() + ps.getStartLine().getLength() - offset;
            String cls = ps.getDoc().get(offset, len);
            List toks = PyAction.getInsideParentesisToks(cls);
            cls = cls.substring(0, cls.indexOf('(')).trim();
            
            String delim = PyAction.getDelimiter(ps.getDoc());
            String indent = PyAction.getStaticIndentationString();
            
            String ret = delim+"class "+cls+"(object):"+delim;
            ret += indent+"'''"+delim;
            ret += indent+"'''"+delim;
            ret += indent+delim;
            ret += indent+"def __init__(self";
            
            
            for (Iterator iter = toks.iterator(); iter.hasNext();) {
                ret += ", ";
                String element = (String) iter.next();
                ret += element;
            }
            ret += "):"+delim;
            
            ret += indent+indent+"'''"+delim;
            for (Iterator iter = toks.iterator(); iter.hasNext();) {
                String element = (String) iter.next();
                ret += indent+indent+"@param "+element+":"+delim;
            }
            ret += indent+indent+"'''"+delim;
            ret += indent+indent;
            
            
            return ret;
        
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.AbstractAssistCreate#getProposal(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache, int, org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule, org.python.pydev.editor.codecompletion.revisited.modules.SourceModule)
     */
    protected CompletionProposal getProposal(PySelection ps, ImageCache imageCache, int offset, SourceModule s) {
        Image img=null;
        if(imageCache != null)
            img = imageCache.get(UIConstants.ASSIST_NEW_CLASS);
        String methodToCreate = getDeclToCreate(ps, offset);
        CompletionProposal proposal = new SourceModuleProposal(methodToCreate, 0, 0, methodToCreate.length(), img, "Create class in module "+s.getName(), null, null, s);
        return proposal;
    }
}
