/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist.heuristics;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * Proposal for making an assign to a variable or to a field when some method call is found in the current line.
 *
 * @author Fabio
 */
public class AssistAssignCompletionProposal extends PyCompletionProposal {

    /**
     * The related viewer (needed to get into link mode)
     */
    private ISourceViewer sourceViewer;

    public AssistAssignCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, ISourceViewer sourceViewer, ICompareContext compareContext) {

        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, compareContext);
        this.sourceViewer = sourceViewer;

    }

    @Override
    public void apply(IDocument document) {
        try {
            //default apply
            int lineOfOffset = document.getLineOfOffset(fReplacementOffset);
            document.replace(fReplacementOffset, fReplacementLength, fReplacementString);

            if (SharedCorePlugin.inTestMode()) {
                return;
            }
            int lineOffset = document.getLineOffset(lineOfOffset);
            int lineLength = document.getLineLength(lineOfOffset);
            String lineDelimiter = document.getLineDelimiter(lineOfOffset);
            int lineDelimiterLen = lineDelimiter != null ? lineDelimiter.length() : 0;

            ISourceViewer viewer = sourceViewer;

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

        } catch (Throwable x) {
            // ignore
            Log.log(x);
        }
    }

    @Override
    public Point getSelection(IDocument document) {
        return null;
    }
}
