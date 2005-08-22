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
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistOverride implements IAssistProps {

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache)
     */
    public List getProps(PySelection ps, ImageCache imageCache, File file, PythonNature nature, AbstractNode root) throws BadLocationException {
        ArrayList l = new ArrayList();
        String sel = PyAction.getLineWithoutComments(ps);
        String indentation = PyAction.getStaticIndentationString();
        String delimiter = PyAction.getDelimiter(ps.getDoc());

        String indStart = "";
        int j = sel.indexOf("def ");
        for (int i = 0; i < j; i++) {
            indStart += " ";
        }
        String start = sel.substring(0, j+4);

        
        //code completion to see members of class...
        String[] strs = PyCodeCompletion.getActivationTokenAndQual(ps.getDoc(), ps.getAbsoluteCursorOffset());
        String tok = strs[1];
        CompletionState state = new CompletionState(ps.getStartLineIndex(), ps.getAbsoluteCursorOffset() - ps.getStartLine().getOffset(), null, nature);
        CompletionRequest request = new CompletionRequest(file, nature, ps.getDoc(), "self", ps.getAbsoluteCursorOffset(), 0, new PyCodeCompletion(), "");
        IToken[] selfCompletions = PyCodeCompletion.getSelfCompletions(request, new ArrayList(), state, true);

        
        for (int i = 0; i < selfCompletions.length; i++) {
            IToken token = selfCompletions[i];
            String rep = token.getRepresentation();
            if(rep.startsWith(tok)){
		        StringBuffer buffer = new StringBuffer( start );
                buffer.append(rep);

                String args = token.getArgs();
	            if(args.equals("()")){
	                args = "( self )";
	            }
		        buffer.append(args);
                
                buffer.append(":");
                buffer.append(delimiter);
                
                buffer.append(indStart);
                buffer.append(indentation);
                buffer.append("'''");
                buffer.append(delimiter);

                buffer.append(indStart);
                buffer.append(indentation);
		        buffer.append("@see super method: "+rep);
                buffer.append(delimiter);

                buffer.append(indStart);
                buffer.append(indentation);
                buffer.append("'''");
                buffer.append(delimiter);
                
                buffer.append(indStart);
                buffer.append(indentation);
                
                String comp = buffer.toString();

                l.add(new PyCompletionProposal(comp, ps.getStartLine().getOffset(), ps.getStartLine().getLength(), comp.length() , imageCache.get(UIConstants.ASSIST_NEW_CLASS),
                        rep+" (Override)", null, null, IPyCompletionProposal.PRIORITY_DEFAULT));
            }
        }
        
        return l;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.editor.actions.PySelection, java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel) {
        return sel.indexOf("def ") != -1;
    }

}
