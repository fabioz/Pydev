/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.text.edits.TextEdit;
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
     * 
     * @param editsAlreadyCreated: a map to keep track of the edits that have already been created (so that we don't add
     * the same edit more than once).
     */
    public abstract void fillRefactoringChangeObject(RefactoringRequest request, CheckConditionsContext context, RefactoringStatus status, 
            CompositeChange fChange, Map<Object, ArrayList<TextEdit>> editsAlreadyCreated);

    /**
     * @return a list of entries with the occurrences that will be affected in the refactoring or null if it
     * does not have this kind of association.
     */
    public HashSet<ASTEntry> getOccurrences();

    /**
     * @return a map with the files that will be affected in the refactoring pointing
     * to the entries that will be changed in this process.
     * 
     * The tuple that is the key of the map has the file and the module name that the file represents.
     */
    public Map<Tuple<String, IFile>, HashSet<ASTEntry>> getOccurrencesInOtherFiles();
}