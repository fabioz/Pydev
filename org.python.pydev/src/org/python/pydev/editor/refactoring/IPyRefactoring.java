/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;


import org.eclipse.ui.IPropertyListener;
import org.python.pydev.editor.model.ItemPointer;

/**
 * @author Fabio Zadrozny
 */
public interface IPyRefactoring {

	public static final int REFACTOR_RESULT_PROP = 1;

    /**
     * Want to hear Refactoring things?
     * 
     * @param l
     */
    public abstract void addPropertyListener(IPropertyListener l);

    /**
     * Extract method
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param endLine
     * @param endCol
     * @param name
     * @param operation
     */
    public abstract String extract(RefactoringRequest request);

    /**
     * Rename something (class, method, local...)
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param name
     * @param operation
     */
    public abstract String rename(RefactoringRequest request);

    /**
     * Find where something is defined (many results because it may seem something is defined in several places)
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param operation
     * @return
     */
    public abstract ItemPointer[] findDefinition(RefactoringRequest request);

    /**
     * Inline a local variable
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param operation
     * @return
     */
    public abstract String inlineLocalVariable(RefactoringRequest request);

    /**
     * Extract a local variable from something
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param endLine
     * @param endCol
     * @param name
     * @param operation
     * @return
     */
    public abstract String extractLocalVariable(RefactoringRequest request);


    /**
     * This function restarts the shell (if there is one).
     */
    public abstract void restartShell();

    /**
     * This function kills the shell (if there is one).
     */
    public abstract void killShell();

    /**
     * @param lastRefactorResults The lastRefactorResults to set.
     */
    public abstract void setLastRefactorResults(Object[] lastRefactorResults);

    /**
     * @return Returns the lastRefactorResults.
     */
    public abstract Object[] getLastRefactorResults();
}