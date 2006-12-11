/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.IModule;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalyzerVisitor;
import com.python.pydev.refactoring.refactorer.RefactorerFindReferences;
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
     * This is the list that contains only the occurrences for the current document,
     * passed by the request.
     */
    protected List<ASTEntry> docOccurrences = new ArrayList<ASTEntry>();
    
    /**
     * This map contains:
     * key: tuple with module name and the IFile representing that module
     * value: list of ast entries to be replaced in a given file
     */
    protected Map<Tuple<String, IFile>, List<ASTEntry>> fileOccurrences = new HashMap<Tuple<String,IFile>, List<ASTEntry>>();

    /**
     * @param definition the definition on where this rename should be applied (we will find the references based 
     * on this definition).
     */
    public AbstractRenameRefactorProcess(Definition definition){
        this.definition = definition;
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
     * Adds the ocurrences found to some module.
     * 
     * @param oc the occurrences found
     * @param file the file where the occurrences were found
     * @param modName the name of the module that is bounded to the given file.
     */
    protected void addOccurrences(List<ASTEntry> oc, IFile file, String modName) {
        Tuple<String, IFile> key = new Tuple<String, IFile>(modName, file);
        List<ASTEntry> existent = fileOccurrences.get(key);
        if(existent == null){
        	fileOccurrences.put(key, oc);
        }else{
        	existent.addAll(oc);
        }
    }

    /**
     * Gets the occurrences in a document and converts it to a TextEdit as required
     * by the Eclipse language toolkit.
     * 
     * @param ocurrences the occurrences found
     * @param doc the doc where the occurrences were found
     * @return a list of tuples with the TextEdit and the description for that edit.
     */
    protected List<Tuple<TextEdit, String>> getAllRenameEdits(List<ASTEntry> ocurrences, IDocument doc) {
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
            int offset = PySelection.getAbsoluteCursorOffset(doc, entry.node.beginLine-1, entry.node.beginColumn-1);
            if(!s.contains(offset)){
	            s.add(offset);
	            ret.add(new Tuple<TextEdit, String>(createRenameEdit(offset), entryBuf.toString()));
            }
        }
        return ret;
    }

    /**
     * Create a text edit on the given offset.
     * 
     * It uses the information in the request to obtain the length of the replace and
     * the new name to be set in the replace
     * 
     * @param offset the offset marking the place where the replace should happen.
     * @return a TextEdit correponding to a rename.
     */
    protected TextEdit createRenameEdit(int offset) {
        return new ReplaceEdit(offset, request.duringProcessInfo.initialName.length(), request.duringProcessInfo.name);
    }
    
    /**
     * This function is used to redirect where the initial should should target
     * (in the local or workspace scope).
     * 
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.RefactoringStatus, org.python.pydev.editor.refactoring.RefactoringRequest)
     */
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
    
    /**
     * This function should be overriden to find the occurrences in the local scope
     * (and check if they are correct).
     * 
     * @param status object where the status can be set (to add errors/warnings)
     * @param request the request used for this check
     */
    protected void checkInitialOnLocalScope(RefactoringStatus status, RefactoringRequest request) {
        throw new RuntimeException("Not implemented search on local scope:"+this.getClass().getName());
    }
    
    /**
     * This function should be overriden to find the occurrences in the workspace scope
     * (and check if they are correct).
     * 
     * @param status object where the status can be set (to add errors/warnings)
     * @param request the request used for this check
     */
    protected void checkInitialOnWorkspace(RefactoringStatus status, RefactoringRequest request) {
        throw new RuntimeException("Not implemented search on workspace:"+this.getClass().getName());
    }
    

    
    /**
     * In this method, changes from the occurrences found in the current document and 
     * other files are transformed to the objects required by the Eclipse Language Toolkit
     *  
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext, org.eclipse.ltk.core.refactoring.RefactoringStatus, org.eclipse.ltk.core.refactoring.TextChange)
     */
    public void checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context, RefactoringStatus status, CompositeChange fChange) {
        createCurrModuleChange(status, fChange);
        createOtherFileChanges(fChange);
    }

    /**
     * Create the changes for references in other modules.
     * 
     * @param fChange the 'root' change.
     */
    private void createOtherFileChanges(CompositeChange fChange) {
        for(Map.Entry<Tuple<String, IFile>, List<ASTEntry>> entry : fileOccurrences.entrySet()){
            Tuple<String, IFile> tup = entry.getKey();
            IDocument docFromResource = REF.getDocFromResource(tup.o2);
            TextFileChange fileChange = new TextFileChange("RenameChange: "+request.duringProcessInfo.name, tup.o2);
            
            MultiTextEdit rootEdit = new MultiTextEdit();
            fileChange.setEdit(rootEdit);
            fileChange.setKeepPreviewEdits(true);

            for (Tuple<TextEdit, String> t : getAllRenameEdits(entry.getValue(), docFromResource)) {
                rootEdit.addChild(t.o1);
                fileChange.addTextEditGroup(new TextEditGroup(t.o2, t.o1));
            }
            
            fChange.add(fileChange);
        }
    }

    /**
     * Create the change for the current module
     * 
     * @param status the status for the change.
     * @param fChange tho 'root' change.
     */
    private void createCurrModuleChange(RefactoringStatus status, CompositeChange fChange) {
        DocumentChange docChange = new DocumentChange("RenameChange: "+request.duringProcessInfo.name, request.doc);
        if(docOccurrences.size() == 0){
            status.addFatalError("No occurrences found.");
            return;
        }

        MultiTextEdit rootEdit = new MultiTextEdit();
        docChange.setEdit(rootEdit);
        docChange.setKeepPreviewEdits(true);

        for (Tuple<TextEdit, String> t : getAllRenameEdits(docOccurrences, request.ps.getDoc())) {
            rootEdit.addChild(t.o1);
            docChange.addTextEditGroup(new TextEditGroup(t.o2, t.o1));
        }
        fChange.add(docChange);
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
     * Implemented from the super interface. Should return the occurrences from the current document
     *  
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#getOcurrences()
     */
    public List<ASTEntry> getOcurrences() {
        return docOccurrences;
    }

    /**
     * Implemented from the super interface. Should return the occurrences found in other documents
     * (but should not return the ones found in the current document)
     * 
     * @see com.python.pydev.refactoring.wizards.IRefactorProcess#getOccurrencesInOtherFiles()
     */
    public Map<Tuple<String, IFile>, List<ASTEntry>> getOccurrencesInOtherFiles() {
        return this.fileOccurrences;
    }
    

    /**
     * Searches for a list of entries that are found within a scope.
     * 
     * It is always based on a single scope and bases itself on a refactoring request.
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
    
    /**
     * This functions tries to find the modules that may have matches for a given request.
     * 
     * Note that it may return files that don't actually contain what we're looking for.
     * 
     * @param request the rquest for a rename.
     * @return a list with the files that may contain matches for the refactoring.
     */
    protected List<IFile> findFilesWithPossibleReferences(RefactoringRequest request) {
        return new RefactorerFindReferences().findPossibleReferences(request);
    }


}
