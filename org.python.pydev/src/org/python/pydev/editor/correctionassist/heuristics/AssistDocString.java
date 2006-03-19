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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistDocString implements IAssistProps {

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.core.docutils.PySelection, org.python.pydev.core.bundle.ImageCache)
     */
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, PyEdit edit, int offset) throws BadLocationException {
        ArrayList<ICompletionProposal> l = new ArrayList<ICompletionProposal>(); 
        Tuple<List<String>, Integer> tuple = ps.getInsideParentesisToks(false);
        if(tuple == null){
        	tuple = new Tuple<List<String>, Integer>(new ArrayList<String>(), offset);
        }
        List params = tuple.o1;
        
	    String initial = PySelection.getIndentationFromLine(ps.getCursorLineContents());
        String delimiter = PyAction.getDelimiter(ps.getDoc());
        String indentation = PyAction.getStaticIndentationString();
	    String inAndIndent = delimiter+initial+indentation;
	    
	    StringBuffer buf = new StringBuffer();
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

        int lineOfOffset = ps.getLineOfOffset(tuple.o2);
	    String comp = buf.toString();
        int offsetPosToAdd = ps.getEndLineOffset(lineOfOffset);
        
        l.add(new PyCompletionProposal(comp, offsetPosToAdd, 0, newOffset , imageCache.get(UIConstants.ASSIST_DOCSTRING),
                "Make docstring", null, null, IPyCompletionProposal.PRIORITY_DEFAULT));
	    return l;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection, java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        return (sel.indexOf("class ") != -1 || sel.indexOf("def ") != -1) && 
               ((sel.indexOf("(") != -1 && sel.indexOf("(") != -1) || sel.indexOf(':') != -1);
    }

}
