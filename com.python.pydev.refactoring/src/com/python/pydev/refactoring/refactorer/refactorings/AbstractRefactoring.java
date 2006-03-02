/*
 * Created on Mar 1, 2006
 */
package com.python.pydev.refactoring.refactorer.refactorings;

/**
 * This is the base class for refactorings. Every refactoring is meant to subclass it.
 */
public abstract class AbstractRefactoring {

    /**
     * @return an object representing the changes that will be done. This object can be 
     * used to preview the changes, and should contain all the info for that.
     */
    public abstract RefactoryChange getRefactoringChange();

    /**
     * Actually performs the refactoring.
     * @param change the change (that may have been previewed) that does the actual refactoring.
     */
    public abstract void performRefactoring(RefactoryChange change);
}
