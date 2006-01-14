package com.python.pydev.refactoring.refactorer;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule.FindInfo;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.plugin.nature.PythonNature;

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
		AbstractModule mod = AbstractModule.createModuleFromDoc(
										   request.resolveModule(), request.file, request.doc, 
										   (PythonNature)request.nature, request.getBeginLine());
		
		
		String tok = tokenAndQual[0] + tokenAndQual[1];
		List<FindInfo> lFindInfo = new ArrayList<FindInfo>();
		try {
            //2. check findDefinition (SourceModule)
			Definition[] definitions = mod.findDefinition(tok, request.getBeginLine(), request.getBeginCol(), (PythonNature)request.nature, lFindInfo);
			for (Definition definition : definitions) {
				pointers.add(new ItemPointer(definition.module.getFile(),
						new Location(definition.line-1, definition.col-1),
						new Location(definition.line-1, definition.col-1)));
			}
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
            List<IInfo> tokensEqualTo = AdditionalProjectInterpreterInfo.getTokensEqualTo(lookForInterface, (PythonNature) request.nature,
                    AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
            
            System.out.println("tokens with interface:"+lookForInterface);
            for (IInfo info : tokensEqualTo) {
                System.out.println(info);
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
        IPythonNature pythonNature = request.nature;
        if(pythonNature == null){
            throw new RuntimeException("Unable to do refactor because the file is an a project that does not have the pydev nature configured.");
        }
    }


}
