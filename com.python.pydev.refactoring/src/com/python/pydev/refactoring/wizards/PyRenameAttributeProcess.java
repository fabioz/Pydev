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
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public class PyRenameAttributeProcess extends AbstractRefactorProcess{

    private AssignDefinition definition;
    private List<ASTEntry> ocurrences;

    public PyRenameAttributeProcess(Definition definition) {
        this.definition = (AssignDefinition) definition;
    }

    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        ClassDef classDef = this.definition.scope.getClassDef();
        if(classDef == null){
            status.addFatalError("We're trying to rename an instance variable, but we cannot find a class definition.");
        }
        ocurrences = Scope.getAttributeOcurrences(this.definition.target, classDef);
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

        for (Tuple<TextEdit, String> t : getAllRenameEdits(ocurrences)) {
            rootEdit.addChild(t.o1);
            fChange.addTextEditGroup(new TextEditGroup(t.o2, t.o1));
        }
    }

}
