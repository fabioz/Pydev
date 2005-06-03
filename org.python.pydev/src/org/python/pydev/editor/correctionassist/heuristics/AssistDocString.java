/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistDocString implements IAssistProps {

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache)
     */
    public List getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, AbstractNode root) throws BadLocationException {
        ArrayList l = new ArrayList(); 
        List params = PyAction.getInsideParentesisToks(ps.getCursorLineContents(), false);
        
        StringBuffer buf = new StringBuffer();
	    String initial = PyAction.getIndentationFromLine(ps.getCursorLineContents());
        String delimiter = PyAction.getDelimiter(ps.getDoc());
        String indentation = PyAction.getStaticIndentationString();
	    String inAndIndent = delimiter+initial+indentation;
	    
        buf.append(inAndIndent+"'''");
	    int newOffset = buf.length();
	    
        if (ps.getCursorLineContents().indexOf("def ") != -1 && params.size()>0){
	        buf.append(inAndIndent);
		    for (Iterator iter = params.iterator(); iter.hasNext();) {
	            String element = (String) iter.next();
	            buf.append(inAndIndent+"@param ");
	            buf.append(element);
	            buf.append(":");
	        }
        }
	    buf.append(inAndIndent+"'''");
	    buf.append(inAndIndent);

	    String comp = buf.toString();
        l.add(new PyCompletionProposal(comp, ps.getStartLine().getOffset()+ps.getStartLine().getLength(), 0, newOffset , imageCache.get(UIConstants.ASSIST_DOCSTRING),
                "Make docstring", null, null, IPyCompletionProposal.PRIORITY_DEFAULT));
	    return l;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.editor.actions.PySelection, java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel) {
        return (sel.indexOf("class ") != -1 || sel.indexOf("def ") != -1) && 
               (sel.indexOf("(") != -1 && sel.indexOf("(") != -1);
    }

}
