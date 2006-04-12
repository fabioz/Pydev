package com.python.pydev.refactoring.refactorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.python.pydev.core.FindInfo;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.editor.refactoring.TooManyMatchesException;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.SystemPythonNature;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;
import com.python.pydev.refactoring.wizards.PyRenameProcessor;
import com.python.pydev.refactoring.wizards.PyRenameRefactoringWizard;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

public class Refactorer extends AbstractPyRefactoring{

	public String extract(RefactoringRequest request) {
		return null;
	}
	public boolean canExtract() {
		return false;
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
            Tuple<String, Integer> currToken = request.ps.getCurrToken();
            request.duringProcessInfo.initialName = currToken.o1;
            request.duringProcessInfo.initialOffset = currToken.o2;
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
	public boolean canRename() {
		return false;
	}

	public ItemPointer[] findDefinition(RefactoringRequest request) {
		//ok, let's find the definition.
		//1. we have to know what we're looking for (activationToken)
		
		List<ItemPointer> pointers = new ArrayList<ItemPointer>();
		String[] tokenAndQual = request.getTokenAndQual();
		
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
		if(modName == null){
            PydevPlugin.logInfo("Unable to resolve module for find definition request (modName == null).");
			return new ItemPointer[0];
		}
		IModule mod = request.getModule();
		
		
		String tok = tokenAndQual[0] + tokenAndQual[1];
		List<FindInfo> lFindInfo = new ArrayList<FindInfo>();
		try {
            //2. check findDefinition (SourceModule)
			IDefinition[] definitions = mod.findDefinition(tok, request.getBeginLine(), request.getBeginCol(), request.nature, lFindInfo);
			AnalysisPlugin.getAsPointers(pointers, (Definition[]) definitions);
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
            IPythonNature nature = request.nature;
            for (IInfo info : tokensEqualTo) {
                AnalysisPlugin.getDefinitionFromIInfo(pointers, manager, nature, info);
            }
        }
		
		return pointers.toArray(new ItemPointer[0]);
	}
    
    public boolean canFindDefinition() {
		return true;
	}

	
	public boolean canInlineLocalVariable() {
		return false;
	}
	public String inlineLocalVariable(RefactoringRequest request) {
		return null;
	}

	
	public boolean canExtractLocalVariable() {
		return false;
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
    
    
    private void findParentDefinitions(IPythonNature nature, Definition d, List<Definition> definitions, HierarchyNodeModel model) throws Exception {
    	//ok, let's find the parents...
    	for(exprType exp :model.ast.bases){
    		String n = NodeUtils.getFullRepresentationString(exp);
    		final int line = exp.beginLine;
    		final int col = exp.beginColumn+n.length(); //the col must be the last char because it can be a dotted name
    		definitions.addAll(Arrays.asList((Definition[])d.module.findDefinition(n, line, col, nature, new ArrayList<FindInfo>())));
    	}
    }
    
	private void findParents(IPythonNature nature, Definition d, HierarchyNodeModel initialModel, HashSet<HierarchyNodeModel> allFound) throws Exception {
		List<Definition> definitions = new ArrayList<Definition>();
		HashSet<HierarchyNodeModel> foundOnRound = new HashSet<HierarchyNodeModel>();
		foundOnRound.add(initialModel);

		while(foundOnRound.size() > 0){
			HashSet<HierarchyNodeModel> nextRound = new HashSet<HierarchyNodeModel>(foundOnRound);
			foundOnRound.clear();
			
			for (HierarchyNodeModel toFindOnRound : nextRound) {
				findParentDefinitions(nature, d, definitions, toFindOnRound);
				
				//and add a parent for each definition found (this will make up the next search we will do)
				for (Definition definition : definitions) {
					HierarchyNodeModel model2 = createHierarhyNodeFromClassDef(definition);
					if(model2 != null && allFound.contains(model2) == false){
						allFound.add(model2);
						toFindOnRound.parents.add(model2);
						foundOnRound.add(model2);
					}
				}
			}
		}
	}
	
	private void findChildren(RefactoringRequest request, HierarchyNodeModel initialModel, HashSet<HierarchyNodeModel> allFound) {
		//and now the children...
		AbstractAdditionalDependencyInfo infoForProject = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(request.nature.getProject());
		synchronized(infoForProject.completeIndex){
			infoForProject.completeIndex.startGrowAsNeeded(2000); //let's stop the cache misses while we're in this process
			try {
				HashSet<HierarchyNodeModel> foundOnRound = new HashSet<HierarchyNodeModel>();
				foundOnRound.add(initialModel);

				while(foundOnRound.size() > 0){
					HashSet<HierarchyNodeModel> nextRound = new HashSet<HierarchyNodeModel>(foundOnRound);
					foundOnRound.clear();

					for (HierarchyNodeModel toFindOnRound : nextRound) {
						HashSet<SourceModule> modulesToAnalyze = findLikelyModulesWithChildren(request, toFindOnRound, infoForProject);
		
						for (SourceModule module : modulesToAnalyze) {
							SourceModule m = (SourceModule) module;
							Iterator<ASTEntry> entries = EasyASTIteratorVisitor.createClassIterator(m.getAst());
							
							while (entries.hasNext()) {
								ASTEntry entry = entries.next();
								//we're checking for those that have model.name as a parent
								ClassDef def = (ClassDef) entry.node;
								List<String> parentNames = NodeUtils.getParentNames(def, true);
								if (parentNames.contains(toFindOnRound.name)) {
									final HierarchyNodeModel newNode = new HierarchyNodeModel(module.getName(), def);
									if(newNode != null && allFound.contains(newNode) == false){
										toFindOnRound.children.add(newNode);
										allFound.add(newNode);
										foundOnRound.add(newNode);
									}
								}
							}
						}
					}
				}				
				
			} finally{
				infoForProject.completeIndex.stopGrowAsNeeded();
			}
		}
	}
	
	private HashSet<SourceModule> findLikelyModulesWithChildren(RefactoringRequest request, HierarchyNodeModel model, AbstractAdditionalDependencyInfo infoForProject) {
		//get the modules that are most likely to have that declaration.
		HashSet<SourceModule> modulesToAnalyze = new HashSet<SourceModule>();
		List<IInfo> tokensEqualTo = infoForProject.getTokensEqualTo(model.name, AdditionalProjectInterpreterInfo.COMPLETE_INDEX);
		for (IInfo info : tokensEqualTo) {
		    String declaringModuleName = info.getDeclaringModuleName();
		    IModule module = request.nature.getAstManager().getModule(declaringModuleName, request.nature, false);
		    if(module instanceof SourceModule){
		    	modulesToAnalyze.add((SourceModule) module);
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
                
                findParents(request.nature, d, model, allFound);
                findChildren(request, model, allFound);
                return model;
            }
            return null;
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @param d
     * @param model
     * @return
     */
    private HierarchyNodeModel createHierarhyNodeFromClassDef(Definition d) {
        HierarchyNodeModel model = null;
        if(d.ast instanceof ClassDef){
            model = new HierarchyNodeModel(d.module.getName(), (ClassDef) d.ast);
        }
        return model;
    }


}
