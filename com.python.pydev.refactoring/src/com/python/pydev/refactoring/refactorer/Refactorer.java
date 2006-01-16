package com.python.pydev.refactoring.refactorer;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.FindInfo;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.SystemPythonNature;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class Refactorer extends AbstractPyRefactoring{

	public String extract(RefactoringRequest request) {
		return null;
	}
	public boolean canExtract() {
		return false;
	}

	
	public String rename(RefactoringRequest request) {
		return null;
	}
	public boolean canRename() {
		return false;
	}

	



	public ItemPointer[] findDefinition(RefactoringRequest request) {
		//ok, let's find the definition.
		//1. we have to know what we're looking for (activationToken)
		
		List<ItemPointer> pointers = new ArrayList<ItemPointer>();
		String[] tokenAndQual = PyCodeCompletion.getActivationTokenAndQual(request.doc, request.ps.getAbsoluteCursorOffset(), true);
		
		if(request.nature == null){
			//the request is not associated to any project. It is probably a system file. So, let's check it...
			SystemPythonNature systemPythonNature = new SystemPythonNature(PydevPlugin.getPythonInterpreterManager());
			String modName = systemPythonNature.resolveModule(request.file);
			if(modName == null){
				systemPythonNature = new SystemPythonNature(PydevPlugin.getJythonInterpreterManager());
				modName = systemPythonNature.resolveModule(request.file);
			}
			if(modName != null){
				request.nature = systemPythonNature;
				request.name = modName;
			}
		}
		
		String modName = request.resolveModule();
		if(modName == null){
			return new ItemPointer[0];
		}
		IModule mod = AbstractModule.createModuleFromDoc(
										   modName, request.file, request.doc, 
										   request.nature, request.getBeginLine());
		
		
		String tok = tokenAndQual[0] + tokenAndQual[1];
		List<FindInfo> lFindInfo = new ArrayList<FindInfo>();
		try {
            //2. check findDefinition (SourceModule)
			IDefinition[] definitions = mod.findDefinition(tok, request.getBeginLine(), request.getBeginCol(), request.nature, lFindInfo);
			getAsPointers(pointers, (Definition[]) definitions);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        
        if(pointers.size() == 0){
            //didn't find it... maybe it could be a parameter (let's check it out)
//            if(lFindInfo.size() > 0){
//                FindInfo info = lFindInfo.get(0);
//                for (IToken iToken : info.localTokens) {
//                    String rep = iToken.getRepresentation();
//                        System.out.println("It is parameter:"+rep);
//                    }
//                }
//            }

            String lookForInterface = tokenAndQual[1];
            List<IInfo> tokensEqualTo = AdditionalProjectInterpreterInfo.getTokensEqualTo(lookForInterface, request.nature,
                    AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
            
            ICodeCompletionASTManager manager = request.nature.getAstManager();
            for (IInfo info : tokensEqualTo) {
                mod = manager.getModule(info.getDeclaringModuleName(), request.nature, true);
                if(mod != null){
	                //ok, now that we found the module, we have to get the actual definition
	                tok = "";
	                String path = info.getPath();
	                if(path != null && path.length() > 0){
	                    tok = path+".";
	                }
	                tok += info.getName();
	                try {
	                    IDefinition[] definitions = mod.findDefinition(tok, 0, 0, request.nature, new ArrayList<FindInfo>());
	                    getAsPointers(pointers, (Definition[]) definitions);
	                } catch (Exception e) {
	                    throw new RuntimeException(e);
	                }
                }
            }
        }
		
		return pointers.toArray(new ItemPointer[0]);
	}
    /**
     * @param pointers
     * @param definitions
     */
    private void getAsPointers(List<ItemPointer> pointers, Definition[] definitions) {
        for (Definition definition : definitions) {
        	pointers.add(new ItemPointer(definition.module.getFile(),
        			new Location(definition.line-1, definition.col-1),
        			new Location(definition.line-1, definition.col-1)));
        }
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


}
