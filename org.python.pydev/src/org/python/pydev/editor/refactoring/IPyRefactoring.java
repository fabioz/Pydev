/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;


import java.util.List;

import org.eclipse.ui.IPropertyListener;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.model.ItemPointer;

/**
 * @author Fabio Zadrozny
 */
public interface IPyRefactoring {

    public static final int REFACTOR_RESULT_PROP = 1;
    
    /**
     * @return The name for the user that represents this refactoring engine.
     * 
     * This is useful for 'solving conflicts' if more than one refactoring engine provides the same action.
     */
    public String getName();

    /**
     * Want to hear Refactoring things?
     * 
     * @param l
     */
    public void addPropertyListener(IPropertyListener l);

    /**
     * Extract method: does the actual extraction given some request
     * The canExtract() method defines whether the extract method will be used from
     * this engine or not (if false, it will use the default refactoring engine)
     * 
     * @return: A string with the status of the refactoring (it will be analyzed if using the
     * default refactoring cycle for an ERROR: substring and shown to the user if found)
     */
    public String extract(RefactoringRequest request);
    public boolean canExtract();

    /**
     * Rename something (class, method, local...)
     */
    public String rename(RefactoringRequest request);
    public boolean canRename();

    /**
     * Find where something is defined (many results because it may seem something is defined in several places)
     * @return an ItemPointer to some definition
     */
    public ItemPointer[] findDefinition(RefactoringRequest request) throws TooManyMatchesException;
    public boolean canFindDefinition();

    /**
     * Inline a local variable
     */
    public String inlineLocalVariable(RefactoringRequest request);
    public boolean canInlineLocalVariable();

    /**
     * Extract a local variable from something
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
    public void setLastRefactorResults(Tuple<IPyRefactoring, List<String>> lastRefactorResults);

    /**
     * @return Returns the lastRefactorResults.
     */
    public Tuple<IPyRefactoring, List<String>> getLastRefactorResults();
    public void firePropertyChange();

    /**
     * Should throw an exception if it cannot do a refactoring based on the request.
     */
    public void checkAvailableForRefactoring(RefactoringRequest request);

    /**
     * Determines if this refactoring engine should execute things inside an operation or if it will create 
     * the operation and synch changes itself.
     */
    public boolean useDefaultRefactoringActionCycle();

    /**
     * This function checks if this engine is available to make the refactoring in some nature.
     * 
     * @param pythonNature the nature where the refactoring will be applied.
     * @throws RuntimeException with the message of the failure.
     */
    public void canRefactorNature(IPythonNature pythonNature) throws RuntimeException;

}