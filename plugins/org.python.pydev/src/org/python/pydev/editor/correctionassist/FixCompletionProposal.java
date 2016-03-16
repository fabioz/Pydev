/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A new Completion proposal because the default is final.
 * This differs because we can specify a line to be deleted after the completion is processed.
 * 
 * @author Fabio Zadrozny
 */
public class FixCompletionProposal implements ICompletionProposal {

    /** The string to be displayed in the completion proposal popup. */
    private String fDisplayString;
    /** The replacement string. */
    private String fReplacementString;
    /** The replacement offset. */
    private int fReplacementOffset;
    /** The replacement length. */
    private int fReplacementLength;
    /** The cursor position after this proposal has been applied. */
    private int fCursorPosition;
    /** The image to be displayed in the completion proposal popup. */
    private Image fImage;
    /** The context information of this proposal. */
    private IContextInformation fContextInformation;
    /** The additional info of this proposal. */
    private String fAdditionalProposalInfo;
    private int lineToRemove;

    /**
     * Creates a new completion proposal based on the provided information. The replacement string is
     * considered being the display string too. All remaining fields are set to <code>null</code>.
     *
     * @param replacementString the actual string to be inserted into the document
     * @param replacementOffset the offset of the text to be replaced
     * @param replacementLength the length of the text to be replaced
     * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
     */
    public FixCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, int lineToRemove) {
        this(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null,
                lineToRemove);
    }

    /**
     * Creates a new completion proposal. All fields are initialized based on the provided information.
     *
     * @param replacementString the actual string to be inserted into the document
     * @param replacementOffset the offset of the text to be replaced
     * @param replacementLength the length of the text to be replaced
     * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
     * @param image the image to display for this proposal
     * @param displayString the string to be displayed for the proposal
     * @param contextInformation the context information associated with this proposal
     * @param additionalProposalInfo the additional information associated with this proposal
     */
    public FixCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int lineToRemove) {
        Assert.isNotNull(replacementString);
        Assert.isTrue(replacementOffset >= 0);
        Assert.isTrue(replacementLength >= 0);
        Assert.isTrue(cursorPosition >= 0);

        fReplacementString = replacementString;
        fReplacementOffset = replacementOffset;
        fReplacementLength = replacementLength;
        fCursorPosition = cursorPosition;
        fImage = image;
        fDisplayString = displayString;
        fContextInformation = contextInformation;
        fAdditionalProposalInfo = additionalProposalInfo;
        this.lineToRemove = lineToRemove;
    }

    /*
     * @see ICompletionProposal#apply(IDocument)
     */
    @Override
    public void apply(IDocument document) {
        try {
            document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
            if (lineToRemove >= 0 && lineToRemove <= document.getNumberOfLines()) {
                IRegion lineInformation = document.getLineInformation(lineToRemove);
                document.replace(lineInformation.getOffset(), lineInformation.getLength(), "");
            }
        } catch (BadLocationException x) {
            // ignore
        }
    }

    /*
     * @see ICompletionProposal#getSelection(IDocument)
     */
    @Override
    public Point getSelection(IDocument document) {
        if (lineToRemove >= 0 && lineToRemove <= document.getNumberOfLines()) {
            try {
                IRegion lineInformation = document.getLineInformation(lineToRemove);
                int pos = lineInformation.getOffset();
                return new Point(pos, 0);
            } catch (BadLocationException e) {
                return new Point(fReplacementOffset + fCursorPosition, 0);
            }
        } else {
            return new Point(fReplacementOffset + fCursorPosition, 0);
        }
    }

    /*
     * @see ICompletionProposal#getContextInformation()
     */
    @Override
    public IContextInformation getContextInformation() {
        return fContextInformation;
    }

    /*
     * @see ICompletionProposal#getImage()
     */
    @Override
    public Image getImage() {
        return fImage;
    }

    /*
     * @see ICompletionProposal#getDisplayString()
     */
    @Override
    public String getDisplayString() {
        if (fDisplayString != null)
            return fDisplayString;
        return fReplacementString;
    }

    /*
     * @see ICompletionProposal#getAdditionalProposalInfo()
     */
    @Override
    public String getAdditionalProposalInfo() {
        return fAdditionalProposalInfo;
    }

}
