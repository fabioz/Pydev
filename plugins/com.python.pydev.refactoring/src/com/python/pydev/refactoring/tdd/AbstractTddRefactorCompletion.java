/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.ProposalsComparator;
import org.python.pydev.parser.PyParser;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

import com.python.pydev.analysis.builder.AnalysisParserObserver;

/**
 * @author fabioz
 *
 */
public abstract class AbstractTddRefactorCompletion extends PyCompletionProposal implements
        ICompletionProposalExtension2 {

    protected PyEdit edit;

    public AbstractTddRefactorCompletion(PyEdit edit, String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority) {
        this(edit, replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null,
                priority);
    }

    public AbstractTddRefactorCompletion(PyEdit edit, String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, Image image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority) {
        this(edit, replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, ON_APPLY_DEFAULT, "");
    }

    public AbstractTddRefactorCompletion(PyEdit edit, String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, Image image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, new ProposalsComparator.CompareContext(edit));
        this.edit = edit;
    }

    protected void forceReparseInBaseEditorAnd(PyEdit... others) {
        if (edit != null) {
            PyParser parser = edit.getParser();
            parser.forceReparse(
                    new Tuple<String, Boolean>(AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE, true));
        }

        for (PyEdit e : others) {
            PyParser parser = e.getParser();
            parser.forceReparse(
                    new Tuple<String, Boolean>(AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE, true));
        }
    }

}
