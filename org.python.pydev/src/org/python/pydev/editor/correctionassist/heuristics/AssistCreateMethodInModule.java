/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.SourceModuleProposal;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;


/**
 * @author Fabio Zadrozny
 */
public class AssistCreateMethodInModule extends AbstractAssistCreate {

    private String getDeclToCreate(PySelection ps, int offset, String met){
            
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
        
    }

    /**
     * @param ps
     * @param imageCache
     * @param offset
     * @param m
     * @param s
     * @return
     */
    protected PyCompletionProposal getProposal(PySelection ps, ImageCache imageCache, int offset, final SourceModule s) {
        try {
            Image img = null;
            if (imageCache != null)
                img = imageCache.get(UIConstants.ASSIST_NEW_METHOD);
            int len = ps.getStartLine().getOffset() + ps.getStartLine().getLength() - offset;
            String met = ps.getDoc().get(offset, len).trim();

            String methodToCreate = getDeclToCreate(ps, offset, met);
            PyCompletionProposal proposal = new SourceModuleProposal(methodToCreate, 0, 0, methodToCreate.length(), img,
                    "Create method "+met+" in module " + s.getName(), null, null, s, IPyCompletionProposal.PRIORITY_DEFAULT);
            return proposal;
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}







