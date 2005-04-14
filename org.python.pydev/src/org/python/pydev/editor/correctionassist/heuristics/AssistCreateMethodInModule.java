/*
 * Created on Apr 12, 2005
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
public class AssistCreateMethodInModule extends AbstractAssistCreate {

    private String getDeclToCreate(PySelection ps, int offset){
        try {
            int len = ps.getStartLine().getOffset() + ps.getStartLine().getLength() - offset;
            String met = ps.getDoc().get(offset, len).trim();
            
            String delim = PyAction.getDelimiter(ps.getDoc());
            String indent = PyAction.getStaticIndentationString();
            
            String ret = delim+"def "+met+":"+delim;
            ret += indent+"'''"+delim;
            
            List toks = PyAction.getInsideParentesisToks(met);
            
            for (Iterator iter = toks.iterator(); iter.hasNext();) {
                String element = (String) iter.next();
                ret += indent+"@param "+element+":"+delim;
            }
            ret += indent+"'''"+delim;
            ret += indent;
            
            
            return ret;
        
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param ps
     * @param imageCache
     * @param offset
     * @param m
     * @param s
     * @return
     */
    protected CompletionProposal getProposal(PySelection ps, ImageCache imageCache, int offset, final SourceModule s) {
        Image img=null;
        if(imageCache != null)
            img = imageCache.get(UIConstants.ASSIST_NEW_METHOD);
        String methodToCreate = getDeclToCreate(ps, offset);
        CompletionProposal proposal = new SourceModuleProposal(methodToCreate, 0, 0, methodToCreate.length(), img, "Create method in module "+s.getName(), null, null, s);
        return proposal;
    }



}







