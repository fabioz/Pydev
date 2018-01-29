/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.proposals;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.ImageCache;

/**
 * The standard implementation of the <code>ICompletionProposal</code> interface.
 */
public class PyCompletionProposal implements ICompletionProposal, IPyCompletionProposal, ICompletionProposalExtension4,
        ICompletionProposalHandle {

    /** The string to be displayed in the completion proposal popup. */
    protected String fDisplayString;
    /** The replacement string. */
    protected String fReplacementString;
    /** The replacement offset. */
    protected int fReplacementOffset;
    /** The replacement length. */
    protected int fReplacementLength;
    /** The cursor position after this proposal has been applied. */
    protected int fCursorPosition;
    /** The image to be displayed in the completion proposal popup. */
    protected IImageHandle fImage;
    /** The context information of this proposal. */
    protected IContextInformation fContextInformation;
    /** The additional info of this proposal. */
    protected String fAdditionalProposalInfo;
    /** The priority for showing the proposal */
    protected int priority;

    protected ICompareContext fCompareContext;

    @Override
    public ICompareContext getCompareContext() {
        return fCompareContext;
    }

    /**
     * Defines how should the apply be treated
     */
    public int onApplyAction = IPyCompletionProposal.ON_APPLY_DEFAULT;
    public String fArgs;

    /**
     * Creates a new completion proposal based on the provided information. The replacement string is
     * considered being the display string too. All remaining fields are set to <code>null</code>.
     *
     * @param replacementString the actual string to be inserted into the document
     * @param replacementOffset the offset of the text to be replaced
     * @param replacementLength the length of the text to be replaced
     * @param cursorPosition the position of the cursor following the insert relative to replacementOffset
     */
    protected PyCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, int priority, ICompareContext compareContext) {
        this(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null, priority,
                compareContext);
    }

    // Backward-compatibility for jython scripts without compareContext.
    protected PyCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, int priority) {
        this(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null, priority,
                null);
    }

    protected PyCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, IImageHandle image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, ICompareContext compareContext) {
        this(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, IPyCompletionProposal.ON_APPLY_DEFAULT, "",
                compareContext);
    }

    // Backward-compatibility for jython scripts without compareContext.
    protected PyCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, IImageHandle image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority) {
        this(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, IPyCompletionProposal.ON_APPLY_DEFAULT, "", null);
    }

    // Backward-compatibility for jython scripts without compareContext.
    protected PyCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, IImageHandle image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, int onApplyAction, String args) {
        this(replacementString, replacementOffset, replacementLength,
                cursorPosition, image, displayString, contextInformation,
                additionalProposalInfo, priority, onApplyAction, args,
                null);
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
     * @param onApplyAction if we should not actually apply the changes when the completion is applied
     */
    public PyCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, IImageHandle image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, int onApplyAction, String args,
            ICompareContext compareContext) {
        Assert.isNotNull(replacementString);
        Assert.isTrue(replacementOffset >= 0);
        Assert.isTrue(replacementLength >= 0);
        Assert.isTrue(cursorPosition >= 0);

        fReplacementString = replacementString;
        fReplacementOffset = replacementOffset;
        fReplacementLength = replacementLength;
        fCursorPosition = cursorPosition;
        fImage = image;
        if (displayString == null) {
            displayString = replacementString;
        }
        fDisplayString = displayString;
        fContextInformation = contextInformation;
        fAdditionalProposalInfo = additionalProposalInfo;
        this.priority = priority;
        this.onApplyAction = onApplyAction;
        this.fArgs = args;
        this.fCompareContext = compareContext;
    }

    /*
     * @see ICompletionProposal#apply(IDocument)
     */
    @Override
    public void apply(IDocument document) {
        switch (onApplyAction) {
            case IPyCompletionProposal.ON_APPLY_JUST_SHOW_CTX_INFO:
                break;

            case IPyCompletionProposal.ON_APPLY_DEFAULT:
                try {
                    document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
                } catch (BadLocationException x) {
                    // ignore
                }
                break;

            case IPyCompletionProposal.ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS:
                try {
                    String args;
                    if (fArgs.length() > 0) {
                        args = fArgs.substring(1, fArgs.length() - 1); //remove the parenthesis
                        document.replace(fReplacementOffset + fReplacementLength, 0, args);
                    }
                } catch (BadLocationException x) {
                    // ignore
                    Log.log(x);
                }
                break;

            default:
                throw new RuntimeException("Unexpected apply mode:" + onApplyAction);
        }
    }

    public int getReplacementOffset() {
        return fReplacementOffset;
    }

    /*
     * @see ICompletionProposal#getSelection(IDocument)
     */
    @Override
    public Point getSelection(IDocument document) {
        if (onApplyAction == IPyCompletionProposal.ON_APPLY_JUST_SHOW_CTX_INFO) {
            return null;
        }
        if (onApplyAction == IPyCompletionProposal.ON_APPLY_DEFAULT) {
            return new Point(fReplacementOffset + fCursorPosition, 0);
        }
        if (onApplyAction == IPyCompletionProposal.ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS) {
            return new Point(fReplacementOffset + fCursorPosition - 1, 0);
        }
        throw new RuntimeException("Unexpected apply mode:" + onApplyAction);
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
        return ImageCache.asImage(fImage);
    }

    /*
     * @see ICompletionProposal#getDisplayString()
     */
    @Override
    public final String getDisplayString() {
        //        if (fDisplayString == null){
        //            throw new AssertionError("This should NEVER happen!");
        //        }
        return fDisplayString;
    }

    /*
     * @see ICompletionProposal#getAdditionalProposalInfo()
     */
    @Override
    public String getAdditionalProposalInfo() {
        return fAdditionalProposalInfo;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getDisplayString().hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PyCompletionProposal)) {
            return false;
        }
        PyCompletionProposal c = (PyCompletionProposal) obj;
        if (!(getDisplayString().equals(c.getDisplayString()))) {
            return false;
        }
        return true;
    }

    /**
     * @see org.python.pydev.shared_ui.proposals.IPyCompletionProposal#getPriority()
     */
    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean isAutoInsertable() {
        return onApplyAction == IPyCompletionProposal.ON_APPLY_JUST_SHOW_CTX_INFO
                || onApplyAction == IPyCompletionProposal.ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS;
    }

    /**
     * @param curr another completion that has the same internal representation.
     * @return the behavior when faced with a given proposal (that has the same internal representation)
     */
    @Override
    public int getOverrideBehavior(ICompletionProposalHandle curr) {
        return IPyCompletionProposal.BEHAVIOR_OVERRIDES;
    }
}
