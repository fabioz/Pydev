/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistOverride implements IAssistProps {

    /**
     * @throws MisconfigurationException 
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.core.docutils.PySelection, org.python.pydev.core.bundle.ImageCache)
     */
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File file, IPythonNature nature, PyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        ArrayList<ICompletionProposal> l = new ArrayList<ICompletionProposal>();
        if(nature == null){
            return l;
        }
        String sel = PyAction.getLineWithoutComments(ps);
        String indentation = PyAction.getStaticIndentationString(edit);
        String delimiter = PyAction.getDelimiter(ps.getDoc());

        String indStart = "";
        int j = sel.indexOf("def ");
        for (int i = 0; i < j; i++) {
            indStart += " ";
        }
        String start = sel.substring(0, j+4);

        
        //code completion to see members of class...
        String[] strs = PySelection.getActivationTokenAndQual(ps.getDoc(), ps.getAbsoluteCursorOffset(), false);
        String tok = strs[1];
        ICompletionState state = new CompletionState(ps.getStartLineIndex(), ps.getAbsoluteCursorOffset() - ps.getStartLine().getOffset(), null, nature,"");
        CompletionRequest request = new CompletionRequest(file, nature, ps.getDoc(), "self", ps.getAbsoluteCursorOffset(), 0, new PyCodeCompletion(), "");
        List<IToken> selfCompletions = new ArrayList<IToken>();
        PyCodeCompletion.getSelfOrClsCompletions(request, selfCompletions, state, true, false, "self");

        
        FastStringBuffer buffer = new FastStringBuffer();
        for (IToken token:selfCompletions) {
            String rep = token.getRepresentation();
            if(rep.startsWith(tok)){
                buffer.clear();
                buffer.append(start);
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
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection, java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        return sel.indexOf("def ") != -1;
    }

}
