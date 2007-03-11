package com.python.pydev.refactoring;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

/**
 * This is an additional interface for refactoring, so that other actions (and not only the
 * default ones provided in the org.python.pydev can be implemented). 
 * 
 * The additional methods in this interface are related to finding a class hierarchy 
 * (used to populate the hierarchy view)
 */
public interface IPyRefactoring2  {
	
	/**
	 * @return the class hierarchy for some request.
	 */
    public HierarchyNodeModel findClassHierarchy(RefactoringRequest request);
    
    /**
     * @param defs the definitions we're intersted in.
     * @return true if all the definitions are in the same class hierarchy and false otherwise
     */
    public boolean areAllInSameClassHierarchy(List<AssignDefinition> defs);

    /**
     * @return A map so that: the key of the map has the file and the module name that the file represents and 
     * the value a list of occurrences.
     */
    public Map<Tuple<String, IFile>, List<ASTEntry>> findAllOccurrences(RefactoringRequest req) throws OperationCanceledException, CoreException;
}
