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
package org.python.pydev.editor.correctionassist.docstrings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.ui.UIConstants;

public class AssistDocString implements IAssistProps {

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.core.docutils.PySelection,
     *      org.python.pydev.core.bundle.ImageCache)
     */
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature, PyEdit edit, int offset)
            throws BadLocationException {
        ArrayList<ICompletionProposal> l = new ArrayList<ICompletionProposal>();
        Tuple<List<String>, Integer> tuple = ps.getInsideParentesisToks(false);

        if (tuple == null) {
            return l;
        }
        List<String> params = tuple.o1;

        String initial = PySelection.getIndentationFromLine(ps.getCursorLineContents());
        String delimiter = PyAction.getDelimiter(ps.getDoc());
        String indentation = edit!=null?edit.getIndentPrefs().getIndentationString():DefaultIndentPrefs.get().getIndentationString();
        String inAndIndent = delimiter + initial + indentation;

        FastStringBuffer buf = new FastStringBuffer();
        String docStringMarker = DocstringsPrefPage.getDocstringMarker();
        buf.append(inAndIndent + docStringMarker);
        buf.append(inAndIndent);

        int newOffset = buf.length();
        if (ps.isInFunctionLine(true)) {
            for (String paramName : params) {
                if(!PySelection.isIdentifier(paramName)){
                    continue;
                }
                buf.append(inAndIndent + DocstringsPrefPage.getPreferredDocstringStyle() + "param " + paramName + ":");
                if (DocstringsPrefPage.getTypeTagShouldBeGenerated(paramName)) {
                    buf.append(inAndIndent + ":type " + paramName + ":");
                }
            }
        } else {
            // It's a class declaration - do nothing.
        }
        buf.append(inAndIndent + docStringMarker);

        int lineOfOffset = ps.getLineOfOffset(tuple.o2);
        String comp = buf.toString();
        int offsetPosToAdd = ps.getEndLineOffset(lineOfOffset);

        Image image = null; //may be null (testing)
        if(imageCache != null){
            image = imageCache.get(UIConstants.ASSIST_DOCSTRING);
        }
        l.add(new PyCompletionProposal(comp, offsetPosToAdd, 0, newOffset, image, "Make docstring",
                null, null, IPyCompletionProposal.PRIORITY_DEFAULT){
            public void apply(IDocument document) {
                //remove the next line if it is a pass...
                PySelection ps = new PySelection(document, fReplacementOffset);
                int iNextLine = ps.getCursorLine()+1;
                String nextLine = ps.getLine(iNextLine);
                if(nextLine.trim().equals("pass")){
                    ps.deleteLine(iNextLine);
                }
                super.apply(document);
            }
        });
        return l;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection,
     *      java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        return ps.isInFunctionLine(true) || ps.isInClassLine();
    }
    
}
