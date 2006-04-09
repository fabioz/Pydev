/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public class PyRenameLocalProcess extends AbstractRefactorProcess{

    private Definition definition;

    private List<ASTEntry> ocurrences;

    public PyRenameLocalProcess(Definition definition) {
        this.definition = definition;
    }

    /** 
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.RefactoringStatus, org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        Scope scope = definition.scope;
        this.ocurrences = scope.getOcurrences(request.duringProcessInfo.initialName);
        this.request = request;
        if(this.ocurrences.size() == 0){
            status.addFatalError("Could not find any ocurrences of:"+request.duringProcessInfo.initialName);
        }
    }

    /** 
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext, org.eclipse.ltk.core.refactoring.RefactoringStatus, org.eclipse.ltk.core.refactoring.TextChange)
     */
    public void checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, RefactoringStatus status, TextChange fChange) {
        if(ocurrences == null){
            status.addFatalError("No ocurrences found.");
            return;
        }

        MultiTextEdit rootEdit = new MultiTextEdit();
        fChange.setEdit(rootEdit);
        fChange.setKeepPreviewEdits(true);

        for (Tuple<TextEdit, String> t : getAllRenameEdits()) {
            rootEdit.addChild(t.o1);
            fChange.addTextEditGroup(new TextEditGroup(t.o2, t.o1));
        }
    }
    protected List<Tuple<TextEdit, String>> getAllRenameEdits() {
        return getAllRenameEdits(ocurrences);
    }

}
