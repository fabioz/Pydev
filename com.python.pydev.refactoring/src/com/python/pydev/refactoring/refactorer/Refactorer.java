package com.python.pydev.refactoring.refactorer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.python.pydev.core.FindInfo;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.CancelledException;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;
import com.python.pydev.refactoring.IPyRefactoring2;
import com.python.pydev.refactoring.wizards.rename.PyRenameProcessor;
import com.python.pydev.refactoring.wizards.rename.PyRenameRefactoringWizard;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

public class Refactorer extends AbstractPyRefactoring implements IPyRefactoring2{
    //--------------------------------------------------------- things we can do
	public boolean canExtract() {
		return false;
	}
    public boolean canRename() {
        return false;
    }
    public boolean canInlineLocalVariable() {
        return false;
    }
    public boolean canExtractLocalVariable() {
        return false;
    }
    public boolean canFindDefinition() {
        return true;
    }
    //----------------------------------------------------- end things we can do

    
    public String extract(RefactoringRequest request) {
        return null;
    }

	
    /**
     * Renames something... 
     * 
     * Basically passes things to the rename processor (it will choose the kind of rename that will happen). 
     * 
     * @see org.python.pydev.editor.refactoring.IPyRefactoring#rename(org.python.pydev.editor.refactoring.RefactoringRequest)
     */
	public String rename(RefactoringRequest request) {
        try {
            RenameRefactoring renameRefactoring = new RenameRefactoring(new PyRenameProcessor(request));
            request.fillInitialNameAndOffset();
            final PyRenameRefactoringWizard wizard = new PyRenameRefactoringWizard(renameRefactoring, "Rename", "inputPageDescription", request, request.duringProcessInfo.initialName);
            try {
                RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
                op.run(PyAction.getShell(), "Rename Refactor Action");
            } catch (InterruptedException e) {
                // do nothing. User action got cancelled
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        return null;
	}

	public ItemPointer[] findDefinition(RefactoringRequest request) {
		//ok, let's find the definition.
		//1. we have to know what we're looking for (activationToken)
		request.communicateWork("Finding Definition");
		List<ItemPointer> pointers = new ArrayList<ItemPointer>();
		String[] tokenAndQual = PySelection.getActivationTokenAndQual(request.doc, request.ps.getAbsoluteCursorOffset(), true);
		
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
                request.duringProcessInfo.name = modName;
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
		List<FindInfo> lFindInfo = new ArrayList<FindInfo>();
		try {
            //2. check findDefinition (SourceModule)
            ArrayList<IDefinition> selected = new ArrayList<IDefinition>();
            
			IDefinition[] definitions = mod.findDefinition(CompletionState.getEmptyCompletionState(tok, request.nature), request.getBeginLine(), request.getBeginCol(), request.nature, lFindInfo);
            for (IDefinition definition : definitions) {
                boolean doAdd = true;
                if(definition instanceof Definition){
                    Definition d = (Definition) definition;
                    doAdd = !findActualTokenFromImportFromDefinition(request, tok, lFindInfo, selected, d);
                }
                
                if(doAdd){
                    selected.add(definition);
                }
            }
			AnalysisPlugin.getAsPointers(pointers, selected.toArray(new Definition[0]));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        
        if(pointers.size() == 0 && request.findDefinitionInAdditionalInfo){
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
            for (IInfo info : tokensEqualTo) {
                AnalysisPlugin.getDefinitionFromIInfo(pointers, manager, nature, info);
            }
        }
        request.communicateWork(StringUtils.format("Found: %s matches.", pointers.size()));
		
		return pointers.toArray(new ItemPointer[0]);
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
    private boolean findActualTokenFromImportFromDefinition(RefactoringRequest request, String tok, List<FindInfo> lFindInfo, ArrayList<IDefinition> selected, Definition d) throws Exception {
        boolean didFindNewDef = false;
        
        Set<Tuple3<String, Integer, Integer>> whereWePassed = new HashSet<Tuple3<String, Integer, Integer>>();
        
        while(d.ast instanceof ImportFrom){
            Tuple3<String, Integer, Integer> t1 = getTupFromDefinition(d);
            if(t1 == null){
                break;
            }
            whereWePassed.add(t1);
            
            Definition[] found = (Definition[]) d.module.findDefinition(CompletionState.getEmptyCompletionState(tok, request.nature), d.line, d.col, request.nature, lFindInfo);
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
    

	
	public String inlineLocalVariable(RefactoringRequest request) {
		return null;
	}

	
	public String extractLocalVariable(RefactoringRequest request) {
		return null;
	}

	public void restartShell() {
		//no shell
	}

	public void killShell() {
		//no shell
	}

	public void setLastRefactorResults(Object[] lastRefactorResults) {
	}

	public Object[] getLastRefactorResults() {
		return null;
	}

    public void checkAvailableForRefactoring(RefactoringRequest request) {
        //can always do it (does not depend upon the project)
    }
    
    public boolean useDefaultRefactoringActionCycle() {
        return false;
    }
    
    public void findReferences(RefactoringRequest request) {
    }
    
    
    private void findParentDefinitions(IPythonNature nature, Definition d, List<Definition> definitions, List<String> withoutAstDefinitions, HierarchyNodeModel model) throws Exception {
    	//ok, let's find the parents...
    	for(exprType exp :model.ast.bases){
    		String n = NodeUtils.getFullRepresentationString(exp);
    		final int line = exp.beginLine;
    		final int col = exp.beginColumn+n.length(); //the col must be the last char because it can be a dotted name
    		final Definition[] defs = (Definition[])d.module.findDefinition(CompletionState.getEmptyCompletionState(n, nature), line, col, nature, new ArrayList<FindInfo>());
    		if(defs.length > 0){
    			definitions.addAll(Arrays.asList(defs));
    		}else{
    			withoutAstDefinitions.add(n);
    		}
    	}
    }
    
	private void findParents(IPythonNature nature, Definition d, HierarchyNodeModel initialModel, HashSet<HierarchyNodeModel> allFound, RefactoringRequest request) throws Exception {
		HashSet<HierarchyNodeModel> foundOnRound = new HashSet<HierarchyNodeModel>();
		foundOnRound.add(initialModel);

		while(foundOnRound.size() > 0){
			HashSet<HierarchyNodeModel> nextRound = new HashSet<HierarchyNodeModel>(foundOnRound);
			foundOnRound.clear();
			
			for (HierarchyNodeModel toFindOnRound : nextRound) {
				List<Definition> definitions = new ArrayList<Definition>();
				List<String> withoutAstDefinitions = new ArrayList<String>();
				findParentDefinitions(nature, d, definitions, withoutAstDefinitions, toFindOnRound);
				
                request.communicateWork(StringUtils.format("Found: %s parents for: %s", definitions.size(), d.value));
                
				//and add a parent for each definition found (this will make up the next search we will do)
				for (Definition definition : definitions) {
					HierarchyNodeModel model2 = createHierarhyNodeFromClassDef(definition);
					if(model2 != null){
						if(allFound.contains(model2) == false){
							allFound.add(model2);
							toFindOnRound.parents.add(model2);
							foundOnRound.add(model2);
						}else{
							toFindOnRound.parents.add(model2);
						}
					}
				}
				
				for(String def : withoutAstDefinitions){
					toFindOnRound.parents.add(new HierarchyNodeModel(def));
				}
			}
		}
	}
	
	private void findChildren(RefactoringRequest request, HierarchyNodeModel initialModel, HashSet<HierarchyNodeModel> allFound) {
		//and now the children...
		List<AbstractAdditionalDependencyInfo> infoForProject = AdditionalProjectInterpreterInfo.getAdditionalInfoForProjectAndReferencing(request.nature.getProject());
        for (AbstractAdditionalDependencyInfo info : infoForProject) {
            info.completeIndex.startGrowAsNeeded(2000); //let's stop the cache misses while we're in this process
        }
        
		try {
			HashSet<HierarchyNodeModel> foundOnRound = new HashSet<HierarchyNodeModel>();
			foundOnRound.add(initialModel);

			while(foundOnRound.size() > 0){
				HashSet<HierarchyNodeModel> nextRound = new HashSet<HierarchyNodeModel>(foundOnRound);
				foundOnRound.clear();

				for (HierarchyNodeModel toFindOnRound : nextRound) {
					HashSet<SourceModule> modulesToAnalyze = findLikelyModulesWithChildren(request, toFindOnRound, infoForProject);
					request.communicateWork("Likely modules with matches:"+modulesToAnalyze.size());
					findChildrenOnModules(request, allFound, foundOnRound, toFindOnRound, modulesToAnalyze);
                    
				}
			}				
			
		} finally{
		    for (AbstractAdditionalDependencyInfo info : infoForProject) {
		        info.completeIndex.stopGrowAsNeeded();
            }
		}
	}
    
    private void findChildrenOnModules(RefactoringRequest request, HashSet<HierarchyNodeModel> allFound, HashSet<HierarchyNodeModel> foundOnRound, HierarchyNodeModel toFindOnRound, HashSet<SourceModule> modulesToAnalyze) {
        for (SourceModule module : modulesToAnalyze) {
        	SourceModule m = (SourceModule) module;
        	request.communicateWork("Analyzing:"+m.getName());
            
        	Iterator<ASTEntry> entries = EasyASTIteratorVisitor.createClassIterator(m.getAst());
        	
        	while (entries.hasNext()) {
        		ASTEntry entry = entries.next();
        		//we're checking for those that have model.name as a parent
        		ClassDef def = (ClassDef) entry.node;
        		List<String> parentNames = NodeUtils.getParentNames(def, true);
        		if (parentNames.contains(toFindOnRound.name)) {
        			final HierarchyNodeModel newNode = new HierarchyNodeModel(module, def);
        			if(newNode != null){
        				if(allFound.contains(newNode) == false){
        					toFindOnRound.children.add(newNode);
        					allFound.add(newNode);
        					foundOnRound.add(newNode);
        				}else{
        					toFindOnRound.children.add(newNode);
        				}
        			}
        		}
        	}
        }
    }
	
	private HashSet<SourceModule> findLikelyModulesWithChildren(RefactoringRequest request, HierarchyNodeModel model, List<AbstractAdditionalDependencyInfo> infoForProject) {
		//get the modules that are most likely to have that declaration.
		HashSet<SourceModule> modulesToAnalyze = new HashSet<SourceModule>();
        for (AbstractAdditionalDependencyInfo additionalInfo : infoForProject) {
    		List<IInfo> tokensEqualTo = additionalInfo.getTokensEqualTo(model.name, AdditionalProjectInterpreterInfo.COMPLETE_INDEX);
    		for (IInfo info : tokensEqualTo) {
    		    String declaringModuleName = info.getDeclaringModuleName();
                
    		    IModule module = null;
                
                IPythonNature pythonNature = null;
    		    if(additionalInfo instanceof AdditionalProjectInterpreterInfo){
    		        AdditionalProjectInterpreterInfo projectInterpreterInfo = (AdditionalProjectInterpreterInfo) additionalInfo;
                    pythonNature = PythonNature.getPythonNature(projectInterpreterInfo.getProject());
                    
                }
                if(pythonNature == null){
                    pythonNature = request.nature;
                }
                module = pythonNature.getAstManager().getModule(declaringModuleName, pythonNature, false);
                if(module == null && pythonNature != request.nature){
                    module = request.nature.getAstManager().getModule(declaringModuleName, request.nature, false);
                }
                
    		    if(module instanceof SourceModule){
    		    	modulesToAnalyze.add((SourceModule) module);
    		    }
            }
		}
		return modulesToAnalyze;
	}

    /**
     * @return the hierarchy model, having the returned node as our 'point of interest'.
     */
    public HierarchyNodeModel findClassHierarchy(RefactoringRequest request) {
        try {
            request.findDefinitionInAdditionalInfo = false;
            ItemPointer[] pointers = this.findDefinition(request);
            if(pointers.length == 1){
            	//ok, this is the default one.
                Definition d = pointers[0].definition;
                HierarchyNodeModel model = createHierarhyNodeFromClassDef(d);
                
                if(model == null){
                	return null;
                }
        		HashSet<HierarchyNodeModel> allFound = new HashSet<HierarchyNodeModel>();
        		allFound.add(model);
                
                request.communicateWork("Finding superclasses");
                findParents(request.nature, d, model, allFound, request);
                request.communicateWork("Finding subclasses");
                findChildren(request, model, allFound);
                request.communicateWork("Done");
                return model;
            }
            
        } catch (CancelledException e) {
            //ignore
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    
    /**
     * @param d
     * @param model
     * @return
     */
    private HierarchyNodeModel createHierarhyNodeFromClassDef(Definition d) {
        HierarchyNodeModel model = null;
        if(d.ast instanceof ClassDef){
            model = new HierarchyNodeModel(d.module, (ClassDef) d.ast);
        }
        return model;
    }
    
    public boolean areAllInSameClassHierarchy(List<AssignDefinition> defs) {
        return true;
    }
    public void canRefactorNature(IPythonNature pythonNature) throws RuntimeException {
        //do nothing (should throw exception if not available)
    }


}
