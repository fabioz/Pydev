package com.python.pydev.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.jface.util.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

/**
 * The rename parameter is based on the rename function, because it will basically:
 * 1- get the  function definition 
 * 2- get all the references 
 * 
 * 3- change the parameter in the function definition (as well as any references to the
 * parameter inside the function
 * 4- change the parameter in all function calls
 * 
 * 
 * This process will only be available if we can find the function definition
 * (otherwise, we'd have a standard rename any local here)
 * 
 * @author fabioz
 *
 */
public class PyRenameParameterProcess extends PyRenameFunctionProcess{

	public PyRenameParameterProcess(Definition definition) {
		super(); 
		//empty, because we'll actually supply a different definition for the superclass (the method 
		//definition, and not the parameter, which we receive here).
		
		Assert.isNotNull(definition.scope, "The scope for a rename parameter must always be provided.");
		
		FunctionDef node = (FunctionDef) definition.scope.getScopeStack().peek();
		super.definition = new Definition(node.name.beginLine, node.name.beginColumn, ((NameTok)node.name).id, node, definition.scope, definition.module);
		
	}
	
	
	/**
	 * These are the methods that we need to override to change the function occurrences for parameter occurrences
	 */
	protected List<ASTEntry> getEntryOccurrencesInSameModule(RefactoringStatus status, RefactoringRequest request, SimpleNode root) {
		List<ASTEntry> occurrences = super.getEntryOccurrencesInSameModule(status, request, root);
		return getParameterOccurences(occurrences);
	}

	protected List<ASTEntry> getEntryOccurrencesInOtherModule(RefactoringRequest request, SimpleNode root) {
		List<ASTEntry> occurrences = super.getEntryOccurrencesInOtherModule(request, root);
		return getParameterOccurences(occurrences);
	}
	
    protected List<ASTEntry> getEntryOccurrences(RefactoringStatus status, String initialName, SourceModule module) {
        List<ASTEntry> occurrences = super.getEntryOccurrences(status, initialName, module);
        return getParameterOccurences(occurrences);
    }
	
    /**
     * This method changes function occurrences for parameter occurrences
     */
    private List<ASTEntry> getParameterOccurences(List<ASTEntry> occurrences) {
    	for (ASTEntry entry : occurrences) {
			System.out.println(entry);
		}
    	return occurrences;
	}


}
