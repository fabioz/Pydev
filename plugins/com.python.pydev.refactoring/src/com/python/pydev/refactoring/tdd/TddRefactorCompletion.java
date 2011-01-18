package com.python.pydev.refactoring.tdd;

import java.util.List;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.refactoring.core.base.RefactoringInfo;

/**
 * This is the proposal that goes outside. It only creates the proposal that'll actually do something later, as
 * creating that proposal may be slower.
 */
public final class TddRefactorCompletion extends PyCompletionProposal implements ICompletionProposalExtension2 {
    private TemplateProposal executed;
    private PyEdit edit;
    private int locationStrategy;
    private List<String> parametersAfterCall;
    private PyCreateAction pyCreateAction;
    private PySelection ps;

    TddRefactorCompletion(String replacementString, 
            Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, 
            int priority, PyEdit edit, int locationStrategy, List<String> parametersAfterCall, PyCreateAction pyCreateAction, PySelection ps) {
        
        super(replacementString, 0, 0, 0, image, displayString, contextInformation,
                additionalProposalInfo, priority);
        this.locationStrategy = locationStrategy;
        this.edit = edit;
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
        return getExecuted().getSelection(document);
    }

    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        getExecuted().apply(viewer, trigger, stateMask, 0);
    }
    
    private TemplateProposal getExecuted() {
        if(executed == null){
            pyCreateAction.setActiveEditor(null, edit);
            try {
                RefactoringInfo refactoringInfo = new RefactoringInfo(edit, ps.getTextSelection());
                executed = (TemplateProposal) pyCreateAction.createProposal(
                        refactoringInfo, this.fReplacementString, this.locationStrategy, parametersAfterCall);
            } catch (MisconfigurationException e) {
                Log.log(e);
            }
        }
        return executed;
    }

    public void selected(ITextViewer viewer, boolean smartToggle) {
    }

    public void unselected(ITextViewer viewer) {
    }

    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        return false;
    }
}