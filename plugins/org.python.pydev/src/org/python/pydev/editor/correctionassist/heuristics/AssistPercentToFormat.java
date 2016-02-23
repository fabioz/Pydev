/******************************************************************************
* Copyright (C) 2011-2013  André Berg and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     André Berg <andre.bergmedia@googlemail.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>           - ongoing maintenance
******************************************************************************/
/*
 * Created on 2011-01-27
 *
 * @author André Berg
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.AbstractTemplateCodeCompletion;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;

public class AssistPercentToFormat extends AbstractTemplateCodeCompletion implements IAssistProps {
    
    private static final boolean DEBUG = false;
    
    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.core.docutils.PySelection,
     *      org.python.pydev.shared_ui.ImageCache)
     */
    @Override
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature, PyEdit edit, int offset)
            throws BadLocationException {
        
        ArrayList<ICompletionProposal> l = new ArrayList<ICompletionProposal>();
        
        String curSelection = ps.getSelectedText();
        
        if (curSelection == null) {
            return l;
        }
                
        curSelection = new String(curSelection);
        
        boolean endsWithLineDelim = false;
        int unchangedLength = curSelection.length();
        if (curSelection.substring(unchangedLength-1, unchangedLength).matches("\\r|\\n") ||
            curSelection.substring(unchangedLength-2, unchangedLength).matches("\\r\\n")) {
            endsWithLineDelim = true;
        }
        
        PercentToBraceConverter ptbc = new PercentToBraceConverter(curSelection);
        String replacementString = ptbc.convert();
        
        if (endsWithLineDelim) {
            replacementString += ps.getEndLineDelim();
        }
        int lenConverted = ptbc.getLength();
        
        int replacementOffset = offset;
        int replacementLength = unchangedLength;
        int cursorPos = replacementOffset + lenConverted;

        if (DEBUG) {
            String sep = System.getProperty("line.separator");
            
            System.out.format(sep + 
                    "Replacement String: %s" + sep + 
                    "Replacement Offset: %d" + sep + 
                    "Replacement Length: %d" + sep + 
                    "Cursor Position:    %d", 
                    replacementString, replacementOffset, replacementLength, cursorPos);
        }

        IRegion region = ps.getRegion();
        TemplateContext context = createContext(edit.getPySourceViewer(), region, ps.getDoc());

        Template t = new Template("Convert", "% to .format()", "", replacementString, false);
        l.add(new TemplateProposal(t, context, region, imageCache.get(UIConstants.COMPLETION_TEMPLATE), 5));
        return l;
    }
    
    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection,
     *      java.lang.String)
     */
    @Override
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        return PercentToBraceConverter.isValidPercentFormatString(ps.getSelectedText(), true);
    }
    
    @Override
    public List<Object> getCodeCompletionProposals(ITextViewer viewer, CompletionRequest request) throws CoreException, BadLocationException {
        throw new RuntimeException("Not implemented: completions should be gotten from the IAssistProps interface.");
    }
}
