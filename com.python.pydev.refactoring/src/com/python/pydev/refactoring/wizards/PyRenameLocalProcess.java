/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public class PyRenameLocalProcess {

    private Definition definition;

    private List<ASTEntry> ocurrences;

    private RefactoringRequest request;

    public PyRenameLocalProcess(Definition definition) {
        this.definition = definition;
    }

    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        Scope scope = definition.scope;
        this.ocurrences = scope.getOcurrences(request.duringProcessInfo.initialName);
        this.request = request;
        if(this.ocurrences.size() == 0){
            status.addFatalError("Could not find any ocurrences of:"+request.duringProcessInfo.initialName);
        }
    }

    public void checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, RefactoringStatus status, TextChange fChange) {
        if(ocurrences == null){
            status.addFatalError("No ocurrences found.");
            return;
        }

        MultiTextEdit rootEdit = new MultiTextEdit();
        fChange.setEdit(rootEdit);
        fChange.setKeepPreviewEdits(true);

        for (TextEdit t : getAllRenameEdits()) {
            rootEdit.addChild(t);
            fChange.addTextEditGroup(new TextEditGroup("changeName", t));
        }
    }
    private List<TextEdit> getAllRenameEdits() {
        List<TextEdit> ret = new ArrayList<TextEdit>();
        for(ASTEntry entry : ocurrences){
            int offset = request.ps.getAbsoluteCursorOffset(entry.node.beginLine-1, entry.node.beginColumn-1);
            ret.add(createRenameEdit(offset));
        }
        return ret;
    }
    private TextEdit createRenameEdit(int offset) {
        return new ReplaceEdit(offset, request.duringProcessInfo.initialName.length(), request.duringProcessInfo.name);
    }

}
