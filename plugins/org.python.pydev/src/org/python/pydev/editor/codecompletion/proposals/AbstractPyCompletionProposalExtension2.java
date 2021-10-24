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
package org.python.pydev.editor.codecompletion.proposals;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension7;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.python.pydev.ast.codecompletion.PyCodeCompletionPreferences;
import org.python.pydev.ast.codecompletion.PyCodeCompletionUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.string.StringUtils;

public abstract class AbstractPyCompletionProposalExtension2 extends AbstractCompletionProposalExtension
        implements ICompletionProposalExtension7 {

    public AbstractPyCompletionProposalExtension2(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority, ICompareContext compareContext) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, priority, compareContext);
    }

    public AbstractPyCompletionProposalExtension2(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args, ICompareContext compareContext) {

        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, onApplyAction, args, compareContext);
    }

    @Override
    protected boolean getApplyCompletionOnDot() {
        return PyCodeCompletionPreferences.applyCompletionOnDot();
    }

    @Override
    public StyledString getStyledDisplayString(IDocument document, int offset, BoldStylerProvider boldStylerProvider) {
        //Extension enabled with enableColoredLabels(true); on PyContentAssistant.
        String[] strs = PySelection.getActivationTokenAndQualifier(document, offset, false);
        if (strs[1].length() == 0 && (strs[0].length() == 0 || strs[0].endsWith("."))) {
            StyledString styledString = new StyledString(getDisplayString());
            return styledString;
        }
        String qualifier = strs[1];

        final boolean useSubstringMatchInCodeCompletion = PyCodeCompletionPreferences
                .getUseSubstringMatchInCodeCompletion();
        String original = getDisplayString();
        // Qualifier is everything after " - ".
        int index = original.indexOf(" - ");
        String strBeforeQualifier;
        if (index != -1) {
            strBeforeQualifier = original.substring(0, index);
        } else {
            strBeforeQualifier = original;
        }

        StyledString styledString = new StyledString();
        if (useSubstringMatchInCodeCompletion) {
            int i = strBeforeQualifier.toLowerCase().indexOf(qualifier.toLowerCase());
            if (i < 0) {
                styledString.append(strBeforeQualifier);
            } else {
                styledString.append(strBeforeQualifier.substring(0, i));
                styledString.append(strBeforeQualifier.substring(i, i + qualifier.length()),
                        boldStylerProvider.getBoldStyler());
                styledString.append(
                        strBeforeQualifier.substring(i + qualifier.length(), strBeforeQualifier.length()));
            }
        } else {
            styledString.append(strBeforeQualifier);
        }
        if (styledString.length() < original.length()) {
            styledString.append(original.substring(styledString.length()), StyledString.QUALIFIER_STYLER);
        }
        return styledString;
    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        String[] strs = PySelection.getActivationTokenAndQualifier(document, offset, false);
        //System.out.println("validating:"+strs[0]+" - "+strs[1]);
        //when we end with a '.', we should start a new completion (and not stay in the old one).
        if (strs[1].length() == 0 && (strs[0].length() == 0 || strs[0].endsWith("."))) {
            //System.out.println(false);
            return false;
        }
        String qualifier = strs[1];
        final boolean useSubstringMatchInCodeCompletion = PyCodeCompletionPreferences
                .getUseSubstringMatchInCodeCompletion();
        String displayString = getDisplayString();
        boolean ret = PyCodeCompletionUtils.acceptName(useSubstringMatchInCodeCompletion, displayString, qualifier);
        return ret;
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
    @Override
    public char[] getTriggerCharacters() {
        char[] chars = VAR_TRIGGER;
        if (PyCodeCompletionPreferences.applyCompletionOnLParen()) {
            chars = StringUtils.addChar(chars, '(');
        }
        if (PyCodeCompletionPreferences.applyCompletionOnRParen()) {
            chars = StringUtils.addChar(chars, ')');
        }
        return chars;
    }
}
