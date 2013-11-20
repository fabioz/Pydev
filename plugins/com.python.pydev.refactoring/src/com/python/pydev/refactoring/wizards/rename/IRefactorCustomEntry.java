package com.python.pydev.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEdit;

public interface IRefactorCustomEntry {

    /**
     * Something as creating a number of "ReplaceEdit(offset, initialName.length(), inputName)"
     * @param inputName 
     * @param initialName 
     */
    List<TextEdit> createRenameEdit(IDocument doc, String initialName, String inputName, RefactoringStatus status);

}
