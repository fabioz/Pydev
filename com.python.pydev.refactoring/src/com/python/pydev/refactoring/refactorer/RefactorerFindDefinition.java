/*
 * Created on Dec 9, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.SystemPythonNature;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Class used to find the definition for some refactoring request.
 * 
 * @author Fabio
 */
public class RefactorerFindDefinition {


    /**
     * This function is used to find the definition for some token.
     * It may return a list of ItemPointer because the actual definition may not be
     * easy to find (so, multiple places that could be the definitions for
     * the given token may be returned... and it may be up to the user to actually
     * choose the best match).
     * 
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#findDefinition(org.python.pydev.editor.refactoring.RefactoringRequest)
     */
    public ItemPointer[] findDefinition(RefactoringRequest request) {
        //ok, let's find the definition.
        //1. we have to know what we're looking for (activationToken)
        request.createSubMonitor(50);
        request.getMonitor().beginTask("Find definition", 5);
        try{
            request.communicateWork("Finding Definition");
            List<ItemPointer> pointers = new ArrayList<ItemPointer>();
            String[] tokenAndQual = PySelection.getActivationTokenAndQual(request.getDoc(), request.ps.getAbsoluteCursorOffset(), true);
            
            String modName = null;
            
            //all that to try to give the user a 'default' interpreter manager, for whatever he is trying to search
            //if it is in some pythonpath, that's easy, but if it is some file somewhere else in the computer, this
            //might turn out a little tricky.
            if(request.nature == null){
                //the request is not associated to any project. It is probably a system file. So, let's check it...
                Tuple<SystemPythonNature,String> infoForFile = PydevPlugin.getInfoForFile(request.file);
                if(infoForFile != null){
                    modName = infoForFile.o2;
                    request.nature = infoForFile.o1;
                    request.inputName = modName;
                }else{
                    return new ItemPointer[0];
                }
            }
            
            
            if(modName == null){
                modName = request.resolveModule();
            }
            
            if(request.nature == null){
                PydevPlugin.logInfo("Unable to resolve nature for find definition request (python or jython interpreter may not be configured).");
                return new ItemPointer[0];
            }
            
            //check if it is already initialized....
            try {
                if(request.nature.isPython()){
                    if(!PydevPlugin.isPythonInterpreterInitialized()){
                        PydevPlugin.logInfo("Python interpreter manager not initialized.");
                        return new ItemPointer[0];
                    }
                }else if(request.nature.isJython()){
                    if(!PydevPlugin.isJythonInterpreterInitialized()){
                        PydevPlugin.logInfo("Jython interpreter manager not initialized.");
                        return new ItemPointer[0];
                    }
                }else{
                    throw new RuntimeException("Project is neither python nor jython?");
                }
            } catch (CoreException e1) {
                throw new RuntimeException(e1);
            }
            //end check if it is already initialized....
            
            IModule mod = request.getModule();
            if(mod == null){
                PydevPlugin.logInfo("Unable to resolve module for find definition request.");
                return new ItemPointer[0];
            }
    
            if(modName == null){
                if(mod.getName() == null){
                    if(mod instanceof SourceModule){
                        SourceModule m = (SourceModule) mod;
                        modName = "__module_not_in_the_pythonpath__";
                        m.setName(modName);
                    }
                }
                if(modName == null){
                    PydevPlugin.logInfo("Unable to resolve module for find definition request (modName == null).");
                    return new ItemPointer[0];
                }
            }
            
            request.communicateWork("Module name found:"+modName);
            
            
            String tok = tokenAndQual[0] + tokenAndQual[1];
            try {
                //2. check findDefinition (SourceModule)
                ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
                
                int beginLine = request.getBeginLine();
				int beginCol = request.getBeginCol()+1;
				IPythonNature pythonNature = request.nature;

				findActualDefinition(request, mod, tok, selected, beginLine, beginCol, pythonNature);
                AnalysisPlugin.getAsPointers(pointers, selected.toArray(new Definition[0]));
                
            } catch (OperationCanceledException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            if(pointers.size() == 0 && ((Boolean)request.getAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_DEFINITION_IN_ADDITIONAL_INFO, true))){
                String lookForInterface = tokenAndQual[1];
                List<IInfo> tokensEqualTo = AdditionalProjectInterpreterInfo.getTokensEqualTo(lookForInterface, request.nature,
                        AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
                
                ICodeCompletionASTManager manager = request.nature.getAstManager();
                if (tokensEqualTo.size() > 100){
                    //too many matches for that...
                    throw new TooManyMatchesException("Too Many matches ("+tokensEqualTo.size()+") were found for the requested token:"+lookForInterface, tokensEqualTo.size());
                }
                request.communicateWork(StringUtils.format("Found: %s possible matches.", tokensEqualTo.size()));
                IPythonNature nature = request.nature;
                CompletionCache completionCache = new CompletionCache();
                for (IInfo info : tokensEqualTo) {
                    AnalysisPlugin.getDefinitionFromIInfo(pointers, manager, nature, info, completionCache);
                    request.checkCancelled();
                }
            }
            request.communicateWork(StringUtils.format("Found: %s matches.", pointers.size()));
            
            return pointers.toArray(new ItemPointer[0]);
        }catch(OperationCanceledException e){
            //that's ok... it was cancelled
            throw e;
        }finally{
            request.getMonitor().done();
            request.popMonitor();
        }
    }

    /**
     * This method will try to find the actual definition given all the needed parameters (but it will not try to find
     * matches in the whole workspace if it's not able to find an exact match in the context)
     * 
     * @param request: used only to communicateWork and checkCancelled
     * @param mod this is the module where we should find the definition
     * @param tok the token we're looking for (complete with dots)
     * @param selected OUT: this is where the definitions should be added
     * @param beginLine starts at 1
     * @param beginCol starts at 1
     * @param pythonNature the nature that we should use to find the definition
     * @throws Exception
     */
	public void findActualDefinition(RefactoringRequest request, IModule mod, String tok, ArrayList<IDefinition> selected, 
	        int beginLine, int beginCol, IPythonNature pythonNature) throws Exception {
	    
	    ICompletionCache completionCache = new CompletionCache();
	    
		IDefinition[] definitions = mod.findDefinition(CompletionStateFactory.getEmptyCompletionState(tok, pythonNature, 
		        beginLine-1, beginCol-1, completionCache), beginLine, beginCol, pythonNature);
		
		request.communicateWork("Found:"+definitions.length+ " definitions");
		for (IDefinition definition : definitions) {
		    boolean doAdd = true;
		    if(definition instanceof Definition){
		        Definition d = (Definition) definition;
		        doAdd = !findActualTokenFromImportFromDefinition(pythonNature, tok, selected, d, completionCache);
		    }
		    request.checkCancelled();
		    if(doAdd){
		        selected.add(definition);
		    }
		}
	}
    
    /** 
     * Given some definition, find its actual token (if that's possible)
     * @param request the original request
     * @param tok the token we're looking for
     * @param lFindInfo place to store info
     * @param selected place to add the new definition (if found)
     * @param d the definition found before (this function will only work if this definition
     * maps to an ImportFrom)
     *  
     * @return true if we found a new definition (and false otherwise)
     * @throws Exception
     */
    private boolean findActualTokenFromImportFromDefinition(IPythonNature nature, String tok, ArrayList<IDefinition> selected, 
            Definition d, ICompletionCache completionCache) throws Exception {
        boolean didFindNewDef = false;
        
        Set<Tuple3<String, Integer, Integer>> whereWePassed = new HashSet<Tuple3<String, Integer, Integer>>();
        
        tok = FullRepIterable.getLastPart(tok); //in an import..from, the last part will always be the token imported 
        
        while(d.ast instanceof ImportFrom){
            Tuple3<String, Integer, Integer> t1 = getTupFromDefinition(d);
            if(t1 == null){
                break;
            }
            whereWePassed.add(t1);
            
            Definition[] found = (Definition[]) d.module.findDefinition(CompletionStateFactory.getEmptyCompletionState(tok, nature, completionCache), d.line, d.col, nature);
            if(found != null && found.length == 1){
                Tuple3<String,Integer,Integer> tupFromDefinition = getTupFromDefinition(found[0]);
                if(tupFromDefinition == null){
                    break;
                }
                if(!whereWePassed.contains(tupFromDefinition)){ //avoid recursions
                    didFindNewDef = true;
                    d = found[0];
                }else{
                    break;
                }
            }else{
                break;
            }
        }
        
        if(didFindNewDef){
            selected.add(d);
        }
        
        return didFindNewDef;
    }
    
    /**
     * @return a tuple with the absolute path to the definition, its line and col.
     */
    private Tuple3<String, Integer, Integer> getTupFromDefinition(Definition d) {
        if(d == null){
            return null;
        }
        File file = d.module.getFile();
        if(file == null){
            return null;
        }
        return new Tuple3<String, Integer, Integer>(REF.getFileAbsolutePath(file), d.line, d.col);
    }

}
