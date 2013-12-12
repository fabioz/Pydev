package com.python.pydev.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.IPythonNature;

public interface IRefactorCustomEntry {

    /**
     * Something as creating a number of "ReplaceEdit(offset, initialName.length(), inputName)"
     * @param inputName 
     * @param initialName 
     * @param workspaceFile 
     * @param nature 
     */
    List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName, RefactoringStatus status,
            IPath workspaceFile, IPythonNature nature);

}
