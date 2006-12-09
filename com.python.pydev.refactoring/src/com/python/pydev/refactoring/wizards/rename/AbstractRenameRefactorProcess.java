/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.IModule;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalyzerVisitor;
import com.python.pydev.refactoring.wizards.IRefactorProcess;

/**
 * This class presents the basic functionality for doing a rename.
 * 
 * @author Fabio
 */
public abstract class AbstractRenameRefactorProcess implements IRefactorProcess{

    /**
     * The request for the refactor
     */
    protected RefactoringRequest request;
    
    /**
     * For a rename, we always need a definition
     */
    protected Definition definition;
    
    /**
     * This map contains:
     * key: tuple with module name and the document representing that module
     * value: list of ast entries to be replaced
     */
    protected List<ASTEntry> docOccurrences = new ArrayList<ASTEntry>();
    protected Map<Tuple<String, IFile>, List<ASTEntry>> fileOccurrences;

    /**
     * @param definition the definition on where this rename should be applied (we will find the references based 
     * on this definition).
     */
    public AbstractRenameRefactorProcess(Definition definition){
        this.definition = definition;
        fileOccurrences = new HashMap<Tuple<String,IFile>, List<ASTEntry>>();
    }
    
    /**
     * Adds the occurences to be renamed given the request. If the rename is a local rename, and there is no need
     * of handling multiple files, this should be the preferred way of adding the occurrences.
     * 
     * @param request will be used to fill the module name and the document
     * @param oc the occurrences to add
     */
    protected void addOccurrences(RefactoringRequest request, List<ASTEntry> oc) {
        docOccurrences.addAll(oc);
    }

    /**
     * @param oc
     * @param doc
     * @param modName
     */
    protected void addOccurrences(List<ASTEntry> oc, IFile doc, String modName) {
        Tuple<String, IFile> key = new Tuple<String, IFile>(modName, doc);
        List<ASTEntry> existent = fileOccurrences.get(key);
        if(existent == null){
        	fileOccurrences.put(key, oc);
        }else{
        	existent.addAll(oc);
        }
    }


    protected List<Tuple<TextEdit, String>> getAllRenameEdits(List<ASTEntry> ocurrences) {
    	Set<Integer> s = new HashSet<Integer>();
    	
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
            if(!s.contains(offset)){
	            s.add(offset);
	            ret.add(new Tuple<TextEdit, String>(createRenameEdit(offset), entryBuf.toString()));
            }
        }
        return ret;
    }

    protected TextEdit createRenameEdit(int offset) {
        return new ReplaceEdit(offset, request.duringProcessInfo.initialName.length(), request.duringProcessInfo.name);
    }
    
    public void checkInitialConditions(IProgressMonitor pm, RefactoringStatus status, RefactoringRequest request) {
        this.request = request;
        
        if(request.findReferencesOnlyOnLocalScope == true){
            checkInitialOnLocalScope(status, request);
            
        }else{
            checkInitialOnWorkspace(status, request);
        }

        if(!occurrencesValid(status)){
            return;
        }
        
    }
    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        throw new RuntimeException("Not implemented search on local scope:"+this.getClass().getName());
    }
    
    protected void checkInitialOnWorkspace(RefactoringStatus status, RefactoringRequest request) {
        throw new RuntimeException("Not implemented search on workspace:"+this.getClass().getName());
    }
    
    /** 
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext, org.eclipse.ltk.core.refactoring.RefactoringStatus, org.eclipse.ltk.core.refactoring.TextChange)
     */
    public void checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, RefactoringStatus status, CompositeChange fChange) {
        DocumentChange docChange = new DocumentChange("RenameChange: "+request.duringProcessInfo.name, request.doc);
        if(docOccurrences.size() == 0){
            status.addFatalError("No occurrences found.");
            return;
        }

        MultiTextEdit rootEdit = new MultiTextEdit();
        docChange.setEdit(rootEdit);
        docChange.setKeepPreviewEdits(true);

        for (Tuple<TextEdit, String> t : getAllRenameEdits(docOccurrences)) {
            rootEdit.addChild(t.o1);
            docChange.addTextEditGroup(new TextEditGroup(t.o2, t.o1));
        }
        fChange.add(docChange);
    }
    
    protected List<Tuple<TextEdit, String>> getAllRenameEdits() {
        return getAllRenameEdits(getOcurrences());
    }
    
    /**
     * Checks if the occurrences gotten are valid or not.
     * 
     * @param status the errors will be added to the passed status.
     * @return true if all is ok and false otherwise
     */
    protected boolean occurrencesValid(RefactoringStatus status){
        if(docOccurrences.size() == 0){
            status.addFatalError("No occurrences found for:"+request.duringProcessInfo.initialName);
            return false;
        }
        return true;
    }
    
    /**
     * Implemented from the super interface. Should only be called when we've looked things in the local scope
     *  
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#getOcurrences()
     */
    public List<ASTEntry> getOcurrences() {
        return docOccurrences;
    }

    

    /**
     * Searches for a list of entries that are found within a scope.
     */
	protected List<ASTEntry> getOccurrencesWithScopeAnalyzer(RefactoringRequest request) {
		List<ASTEntry> entryOccurrences = new ArrayList<ASTEntry>();
    	
        IModule module = request.getModule();
        try {
            ScopeAnalyzerVisitor visitor = new ScopeAnalyzerVisitor(request.nature, request.moduleName, 
                    module, request.doc, new NullProgressMonitor(), request.ps);
            
			request.getAST().accept(visitor);
			entryOccurrences = visitor.getEntryOccurrences();
		} catch (Exception e) {
			Log.log(e);
		}
		return entryOccurrences;
	}

}
