/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 15, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.proposals.AbstractCompletionProposalExtension;

public abstract class AbstractPyCompletionProposalExtension2 extends AbstractCompletionProposalExtension {

    public AbstractPyCompletionProposalExtension2(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, priority);
    }

    public AbstractPyCompletionProposalExtension2(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, Image image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args) {

        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, onApplyAction, args);
    }

    @Override
    protected boolean getApplyCompletionOnDot() {
        return PyCodeCompletionPreferencesPage.applyCompletionOnDot();
    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        String[] strs = PySelection.getActivationTokenAndQual(document, offset, false);
        //System.out.println("validating:"+strs[0]+" - "+strs[1]);
        String qualifier = strs[1].toLowerCase();
        //when we end with a '.', we should start a new completion (and not stay in the old one).
        if (strs[1].length() == 0 && (strs[0].length() == 0 || strs[0].endsWith("."))) {
            //System.out.println(false);
            return false;
        }
        String displayString = getDisplayString().toLowerCase();
        if (displayString.startsWith(qualifier)) {
            //System.out.println(true);
            return true;
        }

        //System.out.println(false);
        return false;
    }

    //-------------------- ICompletionProposalExtension

    //Note that '.' is always there!!
    protected final static char[] VAR_TRIGGER = new char[] { '.' };

    /**
     * We want to apply it on \n or on '.'
     * 
     * When . is entered, the user will finish (and apply) the current completion
     * and request a new one with '.'
     * 
     * If not added, it won't request the new one (and will just stop the current)
     */
    public char[] getTriggerCharacters() {
        char[] chars = VAR_TRIGGER;
        if (PyCodeCompletionPreferencesPage.applyCompletionOnLParen()) {
            chars = StringUtils.addChar(chars, '(');
        }
        if (PyCodeCompletionPreferencesPage.applyCompletionOnRParen()) {
            chars = StringUtils.addChar(chars, ')');
        }
        return chars;
    }
}
