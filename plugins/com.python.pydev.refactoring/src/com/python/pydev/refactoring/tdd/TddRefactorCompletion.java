/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.List;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.ast.refactoring.RefactoringInfo;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.codecompletion.PyTemplateProposal;
import org.python.pydev.parser.PyParser;
import org.python.pydev.shared_core.image.IImageHandle;

import com.python.pydev.analysis.refactoring.tdd.AbstractPyCreateAction;
import com.python.pydev.analysis.refactoring.tdd.TemplateInfo;

/**
 * This is the proposal that goes outside. It only creates the proposal that'll actually do something later, as
 * creating that proposal may be slower.
 */
public final class TddRefactorCompletion extends AbstractTddRefactorCompletion {
    private PyTemplateProposal executed;
    private int locationStrategy;
    private List<String> parametersAfterCall;
    private AbstractPyCreateAction pyCreateAction;
    private PySelection ps;
    private TemplateInfo templateInfo;

    TddRefactorCompletion(String replacementString, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, IPyEdit edit,
            int locationStrategy, List<String> parametersAfterCall, AbstractPyCreateAction pyCreateAction,
            PySelection ps) {

        super(edit, replacementString, 0, 0, 0, image, displayString, contextInformation, additionalProposalInfo,
                priority);
        this.locationStrategy = locationStrategy;
        this.parametersAfterCall = parametersAfterCall;
        this.pyCreateAction = pyCreateAction;
        this.ps = ps;
    }

    @Override
    public void apply(IDocument document) {
        Log.log("This apply should not be called as it implements ICompletionProposalExtension2.");
    }

    @Override
    public boolean isAutoInsertable() {
        return false;
    }

    @Override
    public Point getSelection(IDocument document) {
        TemplateProposal executed2 = getAsTemplateProposal();
        if (executed2 != null) {
            return executed2.getSelection(document);
        }
        return null;
    }

    public PyTemplateProposal convertToTemplateProposal(TemplateInfo templateInfo) {
        return (PyTemplateProposal) CompletionProposalFactory.get().createPyTemplateProposal(templateInfo.fTemplate,
                templateInfo.fContext,
                templateInfo.fRegion, null, 0);
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        if (edit != null) {
            //We have to reparse to make sure that we'll have an accurate AST.
            PyParser parser = (PyParser) edit.getParser();
            if (parser != null) {
                parser.reparseDocument();
            }
        }
        PyTemplateProposal executed2 = getAsTemplateProposal();
        if (executed2 != null) {
            executed2.apply(viewer, trigger, stateMask, 0);
            forceReparseInBaseEditorAnd();
        }
    }

    public PyTemplateProposal getAsTemplateProposal() {
        if (executed == null) {
            try {
                TemplateInfo templateInfo = getAsTemplateInfo();
                executed = convertToTemplateProposal(templateInfo);
            } catch (MisconfigurationException e) {
                Log.log(e);
            }
        }
        return executed;
    }

    public TemplateInfo getAsTemplateInfo() throws MisconfigurationException {
        if (templateInfo == null) {
            pyCreateAction.setActiveEditor(edit);
            RefactoringInfo refactoringInfo = new RefactoringInfo(edit, ps.getTextSelection());
            templateInfo = pyCreateAction.createProposal(refactoringInfo, this.fReplacementString,
                    this.locationStrategy, parametersAfterCall);
        }
        return templateInfo;
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