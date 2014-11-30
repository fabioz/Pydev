/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.codecompletion.simpleassist;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.DocCmd;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

import com.python.pydev.codecompletion.ui.CodeCompletionPreferencesPage;

/**
 * by using this assist (with the extension), we are able to just validate it (without recomputing all completions each time).
 * 
 * They are only recomputed on backspace...
 * 
 * @author Fabio
 */
public class SimpleAssistProposal extends PyCompletionProposal implements ICompletionProposalExtension2 {

    private static final Set<String> ADD_SPACE_AND_COLOR_AFTER = new HashSet<String>();

    static {
        ADD_SPACE_AND_COLOR_AFTER.add("if");
        ADD_SPACE_AND_COLOR_AFTER.add("class");
        ADD_SPACE_AND_COLOR_AFTER.add("for");
        ADD_SPACE_AND_COLOR_AFTER.add("while");
        ADD_SPACE_AND_COLOR_AFTER.add("with");
    }
    private static final Set<String> ADD_SPACE_AFTER = new HashSet<String>();

    static {
        ADD_SPACE_AFTER.add("and");
        ADD_SPACE_AFTER.add("assert");
        ADD_SPACE_AFTER.add("del");
        ADD_SPACE_AFTER.add("def");
        ADD_SPACE_AFTER.add("from");
        ADD_SPACE_AFTER.add("global");
        ADD_SPACE_AFTER.add("import");
        ADD_SPACE_AFTER.add("lambda");
        ADD_SPACE_AFTER.add("not");
        ADD_SPACE_AFTER.add("raise");
        ADD_SPACE_AFTER.add("yield");
        ADD_SPACE_AFTER.add("print"); //Py3K will be print() and won't be affected

        //not there by default but covered for
        ADD_SPACE_AFTER.add("or");
        ADD_SPACE_AFTER.add("as");
        ADD_SPACE_AFTER.add("in");
        ADD_SPACE_AFTER.add("is");
    }

    public SimpleAssistProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, int priority) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, priority);
    }

    public SimpleAssistProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority);
    }

    private int changeInCursorPos = 0;

    @Override
    public Point getSelection(IDocument document) {
        return new Point(fReplacementOffset + fCursorPosition + changeInCursorPos, 0);
    }

    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        try {
            IDocument doc = viewer.getDocument();
            int dif = offset - fReplacementOffset;

            IAdaptable projectAdaptable;
            if (viewer instanceof IAdaptable) {
                projectAdaptable = (IAdaptable) viewer;
            } else {
                projectAdaptable = new IAdaptable() {

                    @Override
                    public Object getAdapter(Class adapter) {
                        return null;
                    }
                };
            }

            if (fReplacementString.equals("elif")) {
                doc.replace(offset, 0, fReplacementString.substring(dif));

                //check if we should dedent
                PyAutoIndentStrategy strategy = new PyAutoIndentStrategy(projectAdaptable);
                DocCmd cmd = new DocCmd(offset + fReplacementString.length() - dif, 0, " ");
                Tuple<String, Integer> dedented = PyAutoIndentStrategy.autoDedentElif(doc, cmd,
                        strategy.getIndentPrefs());
                doc.replace(cmd.offset, 0, " :");
                //make up for the ' :' (right before ':')
                if (dedented != null) {
                    changeInCursorPos = -dedented.o2 + 1;
                }
                return;

            } else if (fReplacementString.endsWith(":")) { //else:, finally:, except: ...
                //make the replacement for the 'else'
                String replacementString = fReplacementString.substring(0, fReplacementString.length() - 1);
                doc.replace(offset, 0, replacementString.substring(dif));

                //dedent if needed
                PyAutoIndentStrategy strategy = new PyAutoIndentStrategy(projectAdaptable);
                DocCmd cmd = new DocCmd(offset + replacementString.length() - dif, 0, ":");
                Tuple<String, Integer> dedented = PyAutoIndentStrategy.autoDedentAfterColon(doc, cmd,
                        strategy.getIndentPrefs());
                doc.replace(cmd.offset, 0, ":");
                //make up for the ':'
                if (dedented != null) {
                    changeInCursorPos = -dedented.o2;
                }
                return;

            } else if (ADD_SPACE_AFTER.contains(fReplacementString)
                    && CodeCompletionPreferencesPage.addSpaceWhenNeeded()) {
                doc.replace(offset, 0, fReplacementString.substring(dif));

                doc.replace(offset + fReplacementString.length() - dif, 0, " ");
                //make up for the ''
                changeInCursorPos = 1;
                return;

            } else if (ADD_SPACE_AND_COLOR_AFTER.contains(fReplacementString)
                    && CodeCompletionPreferencesPage.addSpaceAndColonWhenNeeded()) {
                //it's something as 'class', 'for', etc, which will start a new block.
                //create it as "class space colon" (if the colon is still not there)
                doc.replace(offset, 0, fReplacementString.substring(dif));

                doc.replace(offset + fReplacementString.length() - dif, 0, " :"); //should we add a ':' here (basically changing ':<ENTER>' for '<SHIFT+ENTER>
                changeInCursorPos = 1; //make up for the ' '
                return;
            }

            if (fReplacementString.equals("print()")) {
                changeInCursorPos = -1;
            }

            //execute default if it still hasn't returned.
            doc.replace(offset, 0, fReplacementString.substring(dif));
        } catch (BadLocationException x) {
            // ignore
        }
    }

    public void selected(ITextViewer viewer, boolean smartToggle) {
    }

    public void unselected(ITextViewer viewer) {
    }

    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        String[] strs = PySelection.getActivationTokenAndQual(document, offset, false);

        String activationToken = strs[0];
        String qualifier = strs[1];

        if (activationToken.equals("") && qualifier.equals("") == false) {
            if (fReplacementString.startsWith(qualifier) && !fReplacementString.equals(qualifier)) {
                return true;
            }
        }

        return false;
    }

}