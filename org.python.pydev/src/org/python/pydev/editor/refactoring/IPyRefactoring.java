/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;

import org.eclipse.ui.IPropertyListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;
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
    public abstract String extract(PyEdit editor, int beginLine, int beginCol, int endLine, int endCol, String name, Operation operation);

    /**
     * Rename something (class, method, local...)
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param name
     * @param operation
     */
    public abstract String rename(PyEdit editor, int beginLine, int beginCol, String name, Operation operation);

    /**
     * Find where something is defined (many results because it may seem something is defined in several places)
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param operation
     * @return
     */
    public abstract ItemPointer[] findDefinition(PyEdit editor, int beginLine, int beginCol, Operation operation);

    /**
     * Inline a local variable
     * 
     * @param editor
     * @param beginLine
     * @param beginCol
     * @param operation
     * @return
     */
    public abstract String inlineLocalVariable(PyEdit editor, int beginLine, int beginCol, Operation operation);

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
    public abstract String extractLocalVariable(PyEdit editor, int beginLine, int beginCol, int endLine, int endCol, String name,
            Operation operation);


    /**
     * This function restarts the shell (if there is one).
     */
    public abstract void restartShell();

    /**
     * This function kills the shell.
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