/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public abstract class AbstractRefactorProcess implements IRefactorProcess{

    protected RefactoringRequest request;
    protected Definition definition;
    
    /**
     * This map contains:
     * key: tuple with module name and the document representing that module
     * value: list of ast entries to be replaced
     */
    protected SortedMap<Tuple<String, IDocument>, List<ASTEntry>> occurrences;

    public AbstractRefactorProcess(Definition definition){
        this.definition = definition;
        occurrences = new TreeMap<Tuple<String,IDocument>, List<ASTEntry>>();
    }
    
    protected void addOccurrences(RefactoringRequest request, List<ASTEntry> oc) {
        Tuple<String, IDocument> key = new Tuple<String, IDocument>(request.moduleName, request.doc);
        occurrences.put(key, oc);
    }


    protected List<Tuple<TextEdit, String>> getAllRenameEdits(List<ASTEntry> ocurrences) {
        List<Tuple<TextEdit, String>> ret = new ArrayList<Tuple<TextEdit, String>>();
        StringBuffer buf = new StringBuffer();
        buf.append("Change: ");
        buf.append(request.duringProcessInfo.initialName);
        buf.append(" >> ");
        buf.append(request.duringProcessInfo.name);
        buf.append(" (line:");
        for(ASTEntry entry : ocurrences){
            StringBuffer entryBuf = new StringBuffer(buf.toString());
            entryBuf.append(entry.node.beginLine);
            entryBuf.append(")");
            int offset = request.ps.getAbsoluteCursorOffset(entry.node.beginLine-1, entry.node.beginColumn-1);
            ret.add(new Tuple<TextEdit, String>(createRenameEdit(offset), entryBuf.toString()));
        }
        return ret;
    }

    protected TextEdit createRenameEdit(int offset) {
        return new ReplaceEdit(offset, request.duringProcessInfo.initialName.length(), request.duringProcessInfo.name);
    }
    
    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        this.request = request;
    }
    
//    public void checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, RefactoringStatus status, CompositeChange fChange) {
//    }
    
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
