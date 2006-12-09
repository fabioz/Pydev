package com.python.pydev.refactoring;

import java.util.List;

import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

/**
 * This is an additional interface for refactoring, so that other actions (and not only the
 * default ones provided in the org.python.pydev can be implemented). 
 * 
 * The additional methods in this interface are related to finding a class hierarchy 
 * (used to populate the hierarchy view)
 */
public interface IPyRefactoring2 {
	
	/**
	 * @return the class hierarchy for some request.
	 */
    public HierarchyNodeModel findClassHierarchy(RefactoringRequest request);
    
    /**
     * @param defs the definitions we're intersted in.
     * @return true if all the definitions are in the same class hierarchy and false otherwise
     */
    public boolean areAllInSameClassHierarchy(List<AssignDefinition> defs);

}
