/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.python.pydev.core.interactive_console.IScriptConsoleViewer;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.proposals.PyCompletionProposal;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * Proposal for making an assign to a variable or to a field when some method call is found in the current line.
 *
 * @author Fabio
 */
public class AssistAssignCompletionProposal extends PyCompletionProposal implements ICompletionProposalExtension2 {

    public AssistAssignCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, IImageHandle image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority) {

        super(fixReplacementString(replacementString), replacementOffset, replacementLength, cursorPosition, image,
                displayString,
                contextInformation, additionalProposalInfo, priority, null);

    }

    private static String fixReplacementString(String replacementString) {
        // We receive it in a template format, but that's not what our internal format expects to apply.
        return new FastStringBuffer(replacementString, 0).replaceFirst("${", "").replaceFirst("}", "").toString();
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        if (SharedCorePlugin.inTestMode()) {
            return;
        }
        try {
            IDocument document = viewer.getDocument();
            int lineOfOffset = document.getLineOfOffset(fReplacementOffset);
            apply(document);
            int lineOffset = document.getLineOffset(lineOfOffset);
            int lineLength = document.getLineLength(lineOfOffset);
            String lineDelimiter = document.getLineDelimiter(lineOfOffset);
            int lineDelimiterLen = lineDelimiter != null ? lineDelimiter.length() : 0;

            if (!(viewer instanceof IScriptConsoleViewer)) {
                // For the script console we don't apply the linking (only let the selection take place).
                LinkedModeModel model = new LinkedModeModel();
                LinkedPositionGroup group = new LinkedPositionGroup();
                //the len-3 is because of the end of the string: " = " because the replacement string is
                //something like "xxx = "

                ProposalPosition proposalPosition = new ProposalPosition(document, fReplacementOffset,
                        fReplacementString.length() - 3, 0, new ICompletionProposal[0]);
                group.addPosition(proposalPosition);

                model.addGroup(group);
                model.forceInstall();
                final LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
                ui.setExitPosition(viewer, lineOffset + lineLength - lineDelimiterLen, 0, Integer.MAX_VALUE);
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        ui.enter();
                    }
                };
                RunInUiThread.async(r);
            }

        } catch (BadLocationException e) {
            Log.log(e);
        }
    }

    @Override
    public void apply(IDocument document) {
        try {
            //default apply
            document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
        } catch (Throwable x) {
            // ignore
            Log.log(x);
        }
    }

    @Override
    public Point getSelection(IDocument document) {
        return new Point(fReplacementOffset, fReplacementString.length() - 3);
    }

    @Override
    public void selected(ITextViewer viewer, boolean smartToggle) {
    }

    @Override
    public void unselected(ITextViewer viewer) {
    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        return false;
    }
}
