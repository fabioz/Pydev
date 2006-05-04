/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.ArrayList;
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
import org.python.pydev.parser.visitors.scope.ASTEntry;

public class PyRenameLocalProcess extends AbstractRefactorProcess{

    private Definition definition;

    private List<ASTEntry> occurrences;

    public PyRenameLocalProcess(Definition definition) {
        this.definition = definition;
    }

    /** 
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.RefactoringStatus, org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
    	this.request = request;
    	if(request.findReferencesOnlyOnLocalScope){
    		if(!definition.module.getName().equals(request.moduleName)){
    			//it was found in another module, but we want to keep things local
    			this.occurrences = Scope.getOcurrences(request.duringProcessInfo.initialName, request.getAST());
    		}
    	}
    	
    	if(this.occurrences == null){
    		//not looked previously
	        Scope scope = definition.scope;
	        this.occurrences = scope.getOcurrences(request.duringProcessInfo.initialName, definition.module);
    	}
    	
        if(this.occurrences.size() == 0){
            status.addFatalError("Could not find any occurrences of:"+request.duringProcessInfo.initialName);
        }
    }

    /** 
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext, org.eclipse.ltk.core.refactoring.RefactoringStatus, org.eclipse.ltk.core.refactoring.TextChange)
     */
    public void checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, RefactoringStatus status, CompositeChange fChange) {
        DocumentChange docChange = new DocumentChange("RenameChange: "+request.duringProcessInfo.name, request.doc);
        if(occurrences == null){
            status.addFatalError("No occurrences found.");
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
        return getAllRenameEdits(occurrences);
    }

    public List<ASTEntry> getOcurrences() {
        return new ArrayList<ASTEntry>(occurrences);
    }

}
