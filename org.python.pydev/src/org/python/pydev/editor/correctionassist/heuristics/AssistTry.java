/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.CompletionProposal;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistTry implements IAssistProps {

    /**
     * @throws BadLocationException
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache)
     */
    public List getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, AbstractNode root) throws BadLocationException {
        
        ArrayList l = new ArrayList();
        String indentation = PyAction.getStaticIndentationString();
        
        int start = ps.getStartLine().getOffset();
        int end = ps.getEndLine().getOffset()+ps.getEndLine().getLength();
        
        String string = ps.getDoc().get(start, end-start);
        String delimiter = PyAction.getDelimiter(ps.getDoc());
        
        int firstCharPosition = PyAction.getFirstCharRelativePosition(ps.getDoc(), start);
        String startIndent = "";
        int i = 0;
        while(i < firstCharPosition){
            startIndent += " ";
            i++;
        }
        
        int finRelNewPos;
        int excRelNewPos;
        string = indentation+ string.replaceAll(delimiter, delimiter+indentation);
        String except = startIndent+"try:"+delimiter+string+delimiter;
        except += startIndent+"except:"+delimiter;
        excRelNewPos = except.length() - delimiter.length() -1;
        except += startIndent+indentation+"raise";

        String finall = startIndent+"try:"+delimiter+string+delimiter;
        finall += startIndent+"finally:"+delimiter;
        finall += startIndent+indentation;
        finRelNewPos = finall.length();
        finall += "pass";

        l.add(new CompletionProposal(except, start, end-start, excRelNewPos, imageCache.get(UIConstants.ASSIST_TRY_EXCEPT),
                "Surround with try..except", null, null));
        
        l.add(new CompletionProposal(finall, start, end-start, finRelNewPos, imageCache.get(UIConstants.ASSIST_TRY_FINNALLY),
                "Surround with try..finally", null, null));

        return l;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.editor.actions.PySelection)
     */
    public boolean isValid(PySelection ps, String sel) {
        return ps.getTextSelection().getLength() > 0;
    }
    
    

}
