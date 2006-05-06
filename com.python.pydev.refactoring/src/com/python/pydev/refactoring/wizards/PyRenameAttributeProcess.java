/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
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

    private AssignDefinition assignDefinition;

    public PyRenameAttributeProcess(Definition definition) {
        super(definition);
        this.assignDefinition = (AssignDefinition) definition;
    }

    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        super.checkInitialConditions(pm, status, request);
        ClassDef classDef = this.assignDefinition.scope.getClassDef();
        if(classDef == null){
            status.addFatalError("We're trying to rename an instance variable, but we cannot find a class definition.");
            return;
        }
        List<ASTEntry> oc = Scope.getAttributeOcurrences(this.assignDefinition.target, classDef);
        if(oc.size() == 0){
            status.addFatalError("Could not find any ocurrences of:"+request.duringProcessInfo.initialName);
            return;
        }
        addOccurrences(request, oc);
    }

    public void checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, RefactoringStatus status, CompositeChange fChange) {
        DocumentChange docChange = new DocumentChange("RenameChange: "+request.duringProcessInfo.name, request.doc);
        if(occurrences == null){
            status.addFatalError("No ocurrences found.");
            return;
        }

        MultiTextEdit rootEdit = new MultiTextEdit();
        docChange.setEdit(rootEdit);
        docChange.setKeepPreviewEdits(true);

        for (Tuple<TextEdit, String> t : getAllRenameEdits(getOcurrences())) {
            rootEdit.addChild(t.o1);
            docChange.addTextEditGroup(new TextEditGroup(t.o2, t.o1));
        }
        fChange.add(docChange);
    }


}
