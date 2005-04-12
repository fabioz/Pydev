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
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.CompletionProposal;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistOverride implements IAssistProps {

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache)
     */
    public List getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, AbstractNode root) throws BadLocationException {
        ArrayList l = new ArrayList();
        String sel = PyAction.getLineWithoutComments(ps);
        
        String indentation = PyBackspace.getStaticIndentationString();
        String delimiter = PyAction.getDelimiter(ps.doc);

        String indStart = "";
        int j = sel.indexOf("def ");

        for (int i = 0; i < j; i++) {
            indStart += " ";
        }
        
        String start = sel.substring(0, j+4);
        
        String[] strs = PyCodeCompletion.getActivationTokenAndQual(ps.doc, ps.absoluteCursorOffset);
        String tok = strs[1];
        
        
        CompletionState state = new CompletionState(ps.startLineIndex, ps.absoluteCursorOffset - ps.startLine.getOffset(), null, nature);
        CompletionRequest request = new CompletionRequest(f, nature, ps.doc, "self", ps.absoluteCursorOffset, 0, new PyCodeCompletion(true), "");
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

                l.add(new CompletionProposal(comp, ps.startLine.getOffset(), ps.startLine.getLength(), comp.length() , imageCache.get(UIConstants.ASSIST_NEW_CLASS),
                        rep+" (Override)", null, null));
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
