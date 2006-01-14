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
    public void addPropertyListener(IPropertyListener l);

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
    public String extract(RefactoringRequest request);
    public boolean canExtract();

    /**
     * Rename something (class, method, local...)
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param name
     * @param operation
     */
    public String rename(RefactoringRequest request);
    public boolean canRename();

    /**
     * Find where something is defined (many results because it may seem something is defined in several places)
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param operation
     * @return
     */
    public ItemPointer[] findDefinition(RefactoringRequest request);
    public boolean canFindDefinition();

    /**
     * Inline a local variable
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param operation
     * @return
     */
    public String inlineLocalVariable(RefactoringRequest request);
    public boolean canInlineLocalVariable();

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
    public String extractLocalVariable(RefactoringRequest request);
    public boolean canExtractLocalVariable();


    /**
     * This function restarts the shell (if there is one).
     */
    public void restartShell();

    /**
     * This function kills the shell (if there is one).
     */
    public void killShell();

    /**
     * @param lastRefactorResults The lastRefactorResults to set.
     */
    public void setLastRefactorResults(Object[] lastRefactorResults);

    /**
     * @return Returns the lastRefactorResults.
     */
    public Object[] getLastRefactorResults();
    public void firePropertyChange();

    /**
     * Should throw an exception if it cannot do a refactoring based on the request.
     */
    public void checkAvailableForRefactoring(RefactoringRequest request);
}