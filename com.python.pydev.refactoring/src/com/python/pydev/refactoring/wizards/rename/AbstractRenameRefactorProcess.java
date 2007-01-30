/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
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
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.ASTEntryWithSourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.AstEntryScopeAnalysisConstants;
import com.python.pydev.analysis.scopeanalysis.ScopeAnalyzerVisitor;
import com.python.pydev.refactoring.changes.PyRenameResourceChange;
import com.python.pydev.refactoring.refactorer.AstEntryRefactorerRequestConstants;
import com.python.pydev.refactoring.refactorer.RefactorerFindReferences;
import com.python.pydev.refactoring.wizards.IRefactorRenameProcess;

/**
 * This class presents the basic functionality for doing a rename.
 * 
 * @author Fabio
 */
public abstract class AbstractRenameRefactorProcess implements IRefactorRenameProcess{

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
     * May be used by subclasses
     */
    public AbstractRenameRefactorProcess(){
    	
    }
    
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
        //ocurrences = sortOccurrences(ocurrences);
        
        
        for(ASTEntry entry : ocurrences){
            StringBuffer entryBuf = new StringBuffer();
            
            Integer loc = (Integer)entry.getAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION, 0);
            
            if(loc == AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_IN_COMMENT){
                entryBuf.append("Change (comment): ");
                
            }else if(loc == AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_IN_STRING){
                entryBuf.append("Change (string): ");
                
            }else{
                entryBuf.append("Change: ");
            }
            entryBuf.append(request.initialName);
            entryBuf.append(" >> ");
            entryBuf.append(request.inputName);
            entryBuf.append(" (line:");
            entryBuf.append(entry.node.beginLine);
            entryBuf.append(")");
            
            
            SimpleNode node = entry.node;
            if(node instanceof ClassDef){
                ClassDef def = (ClassDef) node;
                node = def.name;
            }
            if(node instanceof FunctionDef){
            	FunctionDef def = (FunctionDef) node;
            	node = def.name;
            }
            int offset = PySelection.getAbsoluteCursorOffset(doc, node.beginLine-1, node.beginColumn-1);
            if(!s.contains(offset)){
	            s.add(offset);
	            ret.add(new Tuple<TextEdit, String>(createRenameEdit(offset), entryBuf.toString()));
            }
        }
        return ret;
    }

    /**
     * This method is used to sort the occurrences given the place where they were found
     */
    public static List<ASTEntry> sortOccurrences(List<ASTEntry> ocurrences) {
        ocurrences = new ArrayList<ASTEntry>(ocurrences);

        Collections.sort(ocurrences, new Comparator<ASTEntry>(){

            public int compare(ASTEntry o1, ASTEntry o2) {
                int o1Found = (Integer) o1.getAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION, 0);  
                int o2Found = (Integer) o2.getAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION, 0);  
                if(o1Found == o2Found){
                    return 0;
                }
                if(o1Found < o2Found){
                    return -1;
                }else{
                    return 1;
                }
            }});
        return ocurrences;
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
        return new ReplaceEdit(offset, request.initialName.length(), request.inputName);
    }
    
    /**
     * This function is used to redirect where the initial should should target
     * (in the local or workspace scope).
     */
    public void findReferencesToRename(RefactoringRequest request, RefactoringStatus status) {
        this.request = request;
        
        if((Boolean)request.getAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, false)){
            findReferencesToRenameOnLocalScope(request, status);
            
        }else{
            findReferencesToRenameOnWorkspace(request, status);
        }

        if(!occurrencesValid(status)){
            return;
        }
        
    }

    
    /**
     * In this method, changes from the occurrences found in the current document and 
     * other files are transformed to the objects required by the Eclipse Language Toolkit
     */
    public void fillRefactoringChangeObject(RefactoringRequest request, CheckConditionsContext context, RefactoringStatus status, CompositeChange fChange) {
        createCurrModuleChange(status, fChange);
        createOtherFileChanges(fChange, status);
    }
    
    /**
     * This function should be overriden to find the occurrences in the local scope
     * (and check if they are correct).
     * 
     * @param status object where the status can be set (to add errors/warnings)
     * @param request the request used for this check
     */
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        throw new RuntimeException("Not implemented search on local scope:"+this.getClass().getName());
    }
    
    /**
     * This function should be overriden to find the occurrences in the workspace scope
     * (and check if they are correct).
     * 
     * @param status object where the status can be set (to add errors/warnings)
     * @param request the request used for this check
     */
    protected void findReferencesToRenameOnWorkspace(RefactoringRequest request, RefactoringStatus status) {
        throw new RuntimeException("Not implemented search on workspace:"+this.getClass().getName());
    }
    

    /**
     * Create the changes for references in other modules.
     * 
     * @param fChange the 'root' change.
     * @param status the status of the change
     */
    private void createOtherFileChanges(CompositeChange fChange, RefactoringStatus status) {
        for(Map.Entry<Tuple<String, IFile>, List<ASTEntry>> entry : fileOccurrences.entrySet()){
            //key = module name, IFile for the module (__init__ file may be found if it is a package)
            Tuple<String, IFile> tup = entry.getKey();
            
            //check the text changes
            List<ASTEntry> astEntries = filterAstEntries(entry.getValue(), AST_ENTRIES_FILTER_TEXT);
            if(astEntries.size() > 0){
                IDocument docFromResource = REF.getDocFromResource(tup.o2);
                TextFileChange fileChange = new TextFileChange("RenameChange: "+request.inputName, tup.o2);
                
                MultiTextEdit rootEdit = new MultiTextEdit();
                fileChange.setEdit(rootEdit);
                fileChange.setKeepPreviewEdits(true);
    
                for (Tuple<TextEdit, String> t : getAllRenameEdits(astEntries, docFromResource)) {
                    rootEdit.addChild(t.o1);
                    fileChange.addTextEditGroup(new TextEditGroup(t.o2, t.o1));
                }
                
                fChange.add(fileChange);
            }
            
            //now, check for file changes
            astEntries = filterAstEntries(entry.getValue(), AST_ENTRIES_FILTER_FILE);
            if(astEntries.size() > 0){
                IResource resourceToRename = tup.o2;
                String newName = request.inputName+".py";
                
                //if we have an __init__ file but the initial token is not an __init__ file, it means
                //that we have to rename the folder that contains the __init__ file
                if(tup.o1.endsWith(".__init__") && !request.initialName.equals("__init__")){
                    resourceToRename = resourceToRename.getParent();
                    newName = request.inputName;
                    
                    if(!resourceToRename.getName().equals(request.initialName)){
                        status.addFatalError(StringUtils.format("Error. The package that was found (%s) for renaming does not match the initial token found (%s)", 
                                resourceToRename.getName(), request.initialName));
                        return;
                    }
                }
                
                fChange.add(new PyRenameResourceChange(resourceToRename, newName, StringUtils.format("Renaming %s to %s", 
                        resourceToRename.getName(), request.inputName)));
            }
        }
    }

    public final static int AST_ENTRIES_FILTER_TEXT = 1;
    public final static int AST_ENTRIES_FILTER_FILE = 2;
    private List<ASTEntry> filterAstEntries(List<ASTEntry> value, int astEntryFilter) {
        ArrayList<ASTEntry> ret = new ArrayList<ASTEntry>();
        
        for (ASTEntry entry : value) {
            if(entry instanceof ASTEntryWithSourceModule){
                if((astEntryFilter & AST_ENTRIES_FILTER_FILE) != 0){
                    ret.add(entry);
                }
            }else{
                if((astEntryFilter & AST_ENTRIES_FILTER_TEXT) != 0){
                    ret.add(entry);
                }
            }
        }
        
        return ret;
    }

    /**
     * Create the change for the current module
     * 
     * @param status the status for the change.
     * @param fChange tho 'root' change.
     */
    private void createCurrModuleChange(RefactoringStatus status, CompositeChange fChange) {
        DocumentChange docChange = new DocumentChange("Current module: "+request.getModule().getName(), request.getDoc());
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
            status.addFatalError("No occurrences found for:"+request.initialName);
            return false;
        }
        return true;
    }
    
    /**
     * Implemented from the super interface. Should return the occurrences from the current document
     *  
     * @see com.python.pydev.refactoring.wizards.IRefactorRenameProcess#getOcurrences()
     */
    public List<ASTEntry> getOcurrences() {
        return docOccurrences;
    }

    /**
     * Implemented from the super interface. Should return the occurrences found in other documents
     * (but should not return the ones found in the current document)
     * 
     * @see com.python.pydev.refactoring.wizards.IRefactorRenameProcess#getOccurrencesInOtherFiles()
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
                    module, new NullProgressMonitor(), request.ps);
            
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
