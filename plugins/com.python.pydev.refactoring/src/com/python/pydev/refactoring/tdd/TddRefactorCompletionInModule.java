/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;

/**
 * This is the proposal that goes outside. It only creates the proposal that'll actually do something later, as
 * creating that proposal may be slower.
 */
public final class TddRefactorCompletionInModule extends AbstractTddRefactorCompletion {

    private File module;
    private List<String> parametersAfterCall;
    private AbstractPyCreateAction pyCreateAction;
    private PySelection ps;
    public int locationStrategy = AbstractPyCreateAction.LOCATION_STRATEGY_END;

    public TddRefactorCompletionInModule(String replacementString, Image image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, PyEdit edit,
            File module, List<String> parametersAfterCall, AbstractPyCreateAction pyCreateAction, PySelection ps) {

        super(edit, replacementString, 0, 0, 0, image, displayString, contextInformation, additionalProposalInfo,
                priority);
        this.module = module;
        this.parametersAfterCall = parametersAfterCall;
        this.pyCreateAction = pyCreateAction;
        this.ps = ps;
    }

    public List<String> getParametersAfterCall() {
        return parametersAfterCall;
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
        return null;
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        //Upon creation, opens the new editor and creates the class.
        PyOpenAction openAction = new PyOpenAction();
        openAction.run(new ItemPointer(module));
        PyEdit pyEdit = (PyEdit) openAction.editor;
        TddRefactorCompletion completion = new TddRefactorCompletion(fReplacementString, fImage, fDisplayString,
                fContextInformation, fAdditionalProposalInfo, 0, pyEdit, locationStrategy, parametersAfterCall,
                pyCreateAction, ps);
        completion.apply(pyEdit.getEditorSourceViewer(), '\n', 0, 0);

        //As the change was done in another module, let's ask for a new code analysis for the current editor,
        //as the new contents should fix the marker which we used for the fix.
        forceReparseInBaseEditorAnd(pyEdit);
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