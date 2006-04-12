package com.python.pydev.refactoring;

import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

/**
 * This is an additional interface for refactoring, so that other actions (and not only the
 * default ones provided in the org.python.pydev can be implemented). 
 */
public interface IPyRefactoring2 {
	
	/**
	 * @return the class hierarchy for some request.
	 */
    public HierarchyNodeModel findClassHierarchy(RefactoringRequest request);

}
