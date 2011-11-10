/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PythonPairMatcher;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.fastparser.ScopesParser;
import org.python.pydev.parser.fastparser.ScopesParser.Scopes;

/**
 * @author fabioz
 *
 */
public class PyScopeSelection extends PyAction {

    public void run(IAction action) {
        try {
            ITextEditor textEditor = getTextEditor();
            IDocument doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
            ITextSelection selection = (ITextSelection) textEditor.getSelectionProvider().getSelection();

            perform(doc, selection);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public void perform(IDocument doc, ITextSelection selection) {
        ITextSelection newSelection = getNewSelection(doc, selection);
        getPyEdit().setSelection(newSelection.getOffset(), newSelection.getLength());
    }

    public ITextSelection getNewSelection(IDocument doc, ITextSelection selection) {
        try {
            PySelection ps = new PySelection(doc, selection);
            String selectedText = ps.getSelectedText();
            if (selectedText.length() == 0) {
                //Select the current word
                Tuple<String, Integer> currToken = ps.getCurrToken();
                if (currToken.o1.length() > 0) {
                    return new TextSelection(currToken.o2, currToken.o1.length());
                } else {
                    char c = '\0';
                    try {
                        c = ps.getCharAtCurrentOffset();
                    } catch (BadLocationException e) {
                        //Ignore (end of document is selected).
                    }
                    if (StringUtils.isClosingPeer(c)) {
                        PythonPairMatcher pairMatcher = new PythonPairMatcher();
                        int openingOffset = pairMatcher.searchForOpeningPeer(ps.getAbsoluteCursorOffset(), StringUtils.getPeer(c), c, doc);
                        if (openingOffset >= 0) {
                            return new TextSelection(openingOffset, ps.getAbsoluteCursorOffset() - openingOffset + 1);
                        }
                    }
                }
            } else {
                //There's already some text selected
                boolean tryMatchWithQualifier = true;
                boolean hasDotSelected = false; //value only valid if tryMatchWithQualifier == true!
                for (int i = 0; i < selectedText.length(); i++) {
                    char c = selectedText.charAt(i);
                    if(c=='.'){
                        hasDotSelected = true;
                        continue;
                    }
                    if (!Character.isJavaIdentifierPart(c)) {
                        tryMatchWithQualifier = false;
                        break;
                    }
                }
                if (tryMatchWithQualifier) {
                    Tuple<String, Integer> currToken = ps.getCurrToken();
                    if (!hasDotSelected && !currToken.o1.equals(selectedText)) {
                        return new TextSelection(currToken.o2, currToken.o1.length());
                    } else {
                        //The selected text is not equal to the current token, let's see if we have to select a full dotted word
                        Tuple<String, Integer> currDottedToken = ps.getCurrDottedStatement();
                        if (!currDottedToken.o1.equals(selectedText)) {
                            return new TextSelection(currDottedToken.o2, currDottedToken.o1.length());
                        }
                    }
                }
            }
            Scopes scopes = ScopesParser.createScopes(doc);
//            System.out.println(scopes.debugString(doc));
            IRegion scope = scopes.getScopeForSelection(selection.getOffset(), selection.getLength());
            if(scope != null){
                return new TextSelection(scope.getOffset(), scope.getLength());
            }
            
        } catch (BadLocationException e) {
            Log.log(e);
        }
        
        return selection;
    }

}
