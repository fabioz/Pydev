/*
 * Created on May 1, 2006
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
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Scope;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public class PyRenameClassProcess extends AbstractRefactorProcess{

    public PyRenameClassProcess(Definition definition) {
        super(definition);
    }

    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        this.request = request;
        if(request.findReferencesOnlyOnLocalScope == true){
            SimpleNode root = request.getAST();
            List<ASTEntry> oc = Scope.getOcurrences(request.duringProcessInfo.initialName, root);
            if(oc.size() == 0){
                status.addFatalError("Could not find any ocurrences of:"+request.duringProcessInfo.initialName);
                return;
            }
            addOccurrences(request, oc);
            
       
        }else{
            throw new RuntimeException("Currently can only get things in the local scope.");
        }
    }


    public void checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, RefactoringStatus status, CompositeChange fChange) {
        super.checkInitialConditions(pm, status, request);
        DocumentChange docChange = new DocumentChange("RenameChange: "+request.duringProcessInfo.name, request.doc);
        if(occurrences == null){
            status.addFatalError("No ocurrences found.");
            return;
        }

        MultiTextEdit rootEdit = new MultiTextEdit();
        docChange.setEdit(rootEdit);
        docChange.setKeepPreviewEdits(true);

        for (Tuple<TextEdit, String> t : getAllRenameEdits()) {
            rootEdit.addChild(t.o1);
            docChange.addTextEditGroup(new TextEditGroup(t.o2, t.o1));
        }
        fChange.add(docChange);
    }

    protected List<Tuple<TextEdit, String>> getAllRenameEdits() {
        if(request.findReferencesOnlyOnLocalScope == true){
            return getAllRenameEdits(getOcurrences());
        }else{
            throw new RuntimeException("Currently can only get things in the local scope.");
        }
    }


}
