/*
 * Created on May 1, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.Assert;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class PyRenameClassMethodProcess extends AbstractRenameMultipleProcess {

    private Definition definition;

    public PyRenameClassMethodProcess(Definition definition) {
        this.definition = definition;
        Assert.isTrue(this.definition.ast instanceof FunctionDef);
    }

    public List<ASTEntry> getOcurrences(String occurencesFor, SimpleNode simpleNode, RefactoringStatus status) {
        List<ASTEntry> ret = new ArrayList<ASTEntry>();
        
        //get the entry for the function itself
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(simpleNode);
        Iterator<ASTEntry> it = visitor.getIterator(FunctionDef.class);
        ASTEntry functionDefEntry = null;
        while(it.hasNext()){
            functionDefEntry = it.next();
            
            if(functionDefEntry.node.beginLine == this.definition.ast.beginLine && 
                    functionDefEntry.node.beginColumn == this.definition.ast.beginColumn){
                
                ret.add(new ASTEntry(functionDefEntry, ((FunctionDef)functionDefEntry.node).name));
                break;
            }
        }
        
        if(functionDefEntry == null){
            status.addFatalError("Unable to find the original definition for the function definition.");
            return ret;
        }
        
        //get the entry for the self.xxx that access that attribute in the class
        SequencialASTIteratorVisitor classVisitor = SequencialASTIteratorVisitor.create(functionDefEntry.parent.node);
        it = classVisitor.getIterator(Attribute.class);
        while(it.hasNext()){
            ASTEntry entry = it.next();
            List<SimpleNode> parts = NodeUtils.getAttributeParts((Attribute) entry.node);
            
            if(NodeUtils.getRepresentationString(parts.get(0)).equals("self") && 
                    NodeUtils.getRepresentationString(parts.get(1)).equals(occurencesFor)){
                
                ret.add(entry);
            }
        }
        return ret;
    }
    
    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        this.request = request;
        if(request.findReferencesOnlyOnLocalScope == true){
            SimpleNode root = request.getAST();
            Tuple<String, IDocument> key = new Tuple<String, IDocument>(request.moduleName, request.doc);
            occurrences.put(key, getOcurrences(request.duringProcessInfo.initialName, root, status));
            
            if(occurrences.size() == 0){
                status.addFatalError("Could not find any ocurrences of:"+request.duringProcessInfo.initialName);
            }
       
        }else{
            throw new RuntimeException("Currently can only get things in the local scope.");
        }
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

    public List<ASTEntry> getOcurrences() {
        if(occurrences == null){
            return null;
        }
        if(occurrences.size() > 1){
            throw new RuntimeException("This interface cannot get the occurrences for multiple modules.");
        }
        if(occurrences.size() == 1){
            return occurrences.values().iterator().next();
        }
        return null;
    }

}
