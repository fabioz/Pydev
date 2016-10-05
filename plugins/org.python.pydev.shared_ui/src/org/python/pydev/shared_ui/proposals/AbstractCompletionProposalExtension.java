/******************************************************************************
* Copyright (C) 2006-2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
*     Jonah Graham <jonah@kichwacoders.com> - ongoing maintenance
******************************************************************************/
package org.python.pydev.shared_ui.proposals;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.shared_core.log.Log;

public abstract class AbstractCompletionProposalExtension extends PyCompletionProposal implements
        ICompletionProposalExtension2, ICompletionProposalExtension {

    private PyCompletionPresentationUpdater presentationUpdater;

    /**
     * Only available when Ctrl is pressed when selecting the completion.
     */
    public int fLen;

    public boolean fLastIsPar;

    public AbstractCompletionProposalExtension(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority, ICompareContext compareContext) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null, priority,
                compareContext);
    }

    public AbstractCompletionProposalExtension(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, Image image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args, ICompareContext compareContext) {

        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, onApplyAction, args, compareContext);
    }

    /**
     * Called when Ctrl is selected during the completions
     * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer, boolean)
     */
    @Override
    public void selected(ITextViewer viewer, boolean smartToggle) {
        if (smartToggle) {
            StyledText text = viewer.getTextWidget();
            if (text == null || text.isDisposed()) {
                return;
            }

            int widgetCaret = text.getCaretOffset();
            IDocument document = viewer.getDocument();
            int finalOffset = widgetCaret;

            try {
                if (finalOffset >= document.getLength()) {
                    unselected(viewer);
                    return;
                }
                char c;
                do {
                    c = document.getChar(finalOffset);
                    finalOffset++;
                } while (isValidChar(c) && finalOffset < document.getLength());

                if (c == '(') {
                    fLastIsPar = true;
                } else {
                    fLastIsPar = false;
                }

                if (!isValidChar(c)) {
                    finalOffset--;
                }

                this.fLen = finalOffset - widgetCaret;
                this.getPresentationUpdater().selected(viewer, widgetCaret, this.fLen);
            } catch (BadLocationException e) {
                Log.log(e);
            }

        } else {
            unselected(viewer);
        }
    }

    /**
     * @param c
     * @return
     */
    private boolean isValidChar(char c) {
        return Character.isJavaIdentifierPart(c);
    }

    @Override
    public void unselected(ITextViewer viewer) {
        this.getPresentationUpdater().unselected(viewer);
    }

    @Override
    public abstract boolean validate(IDocument document, int offset, DocumentEvent event);

    @Override
    public void apply(IDocument document, char trigger, int offset) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getContextInformationPosition() {
        return this.fCursorPosition;
    }

    @Override
    public boolean isValidFor(IDocument document, int offset) {
        return validate(document, offset, null);
    }

    protected boolean getApplyCompletionOnDot() {
        return false;
    }

    /**
     * Checks if the trigger character should actually a
     * @param trigger
     * @param doc
     * @param offset
     * @return
     */
    protected boolean triggerCharAppliesCurrentCompletion(char trigger, IDocument doc, int offset) {
        if (trigger == '.' && !getApplyCompletionOnDot()) {
            //do not apply completion when it's triggered by '.', because that's usually not what's wanted
            //e.g.: if the user writes sys and the current completion is SystemError, pressing '.' will apply
            //the completion, but what the user usually wants is just having sys.xxx and not SystemError.xxx
            try {
                doc.replace(offset, 0, ".");
            } catch (BadLocationException e) {
                Log.log(e);
            }
            return false;
        }

        return true;
    }

    protected PyCompletionPresentationUpdater getPresentationUpdater() {
        if (presentationUpdater == null) {
            presentationUpdater = new PyCompletionPresentationUpdater();
        }
        return presentationUpdater;
    }

}
