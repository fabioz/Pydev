/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 22/09/2005
 */
package org.python.pydev.editor.correctionassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

public class IgnoreCompletionProposal extends PyCompletionProposal {

    protected final PyEdit edit;

    public IgnoreCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, PyEdit edit) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority);
        this.edit = edit;
    }

    @Override
    public void apply(IDocument document) {
        try {
            //first do the completion
            document.replace(fReplacementOffset, fReplacementLength, fReplacementString);

            //ok, after doing it, let's call for a reparse
            if (edit != null) {
                edit.getParser().forceReparse();
            }
        } catch (BadLocationException x) {
            Log.log(x);
        }
    }

    @Override
    public Point getSelection(IDocument document) {
        return new Point(fCursorPosition, 0); //don't move the cursor
    }

}
