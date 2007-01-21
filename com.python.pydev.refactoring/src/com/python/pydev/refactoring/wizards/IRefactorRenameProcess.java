/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public interface IRefactorRenameProcess {

    /**
     * In this method, implementors should find the references in the workspace (or local scope -- as
     * determined by the request) that should be renamed
     */
    public abstract void findReferencesToRename(RefactoringRequest request, RefactoringStatus status);

    /**
     * In this method, implementors should fill the change object with the renames that were found.
     */
    public abstract void fillRefactoringChangeObject(RefactoringRequest request, CheckConditionsContext context, RefactoringStatus status, CompositeChange fChange);

    /**
     * @return a list of entries with the ocurrences that will be affected in the refactoring or null if it
     * does not have this kind of association.
     */
    public List<ASTEntry> getOcurrences();

    /**
     * @return a map with the files that will be affected in the refactoring pointing
     * to the entries that will be changed in this process.
     * 
     * The tuple that is the key of the map has the file and the module name that the file represents.
     */
    public Map<Tuple<String, IFile>, List<ASTEntry>> getOccurrencesInOtherFiles();
}