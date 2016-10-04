/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;

public class CompletionError implements ICompletionProposal, IPyCompletionProposal, ICompletionProposalExtension4 {

    private Throwable error;

    public CompletionError(Throwable e) {
        this.error = e;
    }

    @Override
    public void apply(IDocument document) {
    }

    @Override
    public String getAdditionalProposalInfo() {
        return getErrorMessage();
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    @Override
    public String getDisplayString() {
        return getErrorMessage();
    }

    @Override
    public Image getImage() {
        return PydevPlugin.getImageCache().get(UIConstants.ERROR);
    }

    @Override
    public Point getSelection(IDocument document) {
        return null;
    }

    @Override
    public int getPriority() {
        return -1;
    }

    @Override
    public boolean isAutoInsertable() {
        return false;
    }

    public String getErrorMessage() {
        String message = error.getMessage();
        if (message == null) {
            //NullPointerException
            if (error instanceof NullPointerException) {
                message = "NullPointerException";
            } else {
                message = "Null error message";
            }
        }

        return message;
    }

    @Override
    public ICompareContext getCompareContext() {
        return null;
    }

}
