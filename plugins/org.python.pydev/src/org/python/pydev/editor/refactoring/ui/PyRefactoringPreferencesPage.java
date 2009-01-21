package org.python.pydev.editor.refactoring.ui;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This editor will config the refactoring engines that are available (so that the user can choose to do 
 * a different action with each refactoring engine)
 * 
 * @author Fabio
 */
public class PyRefactoringPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    protected PyRefactoringPreferencesPage() {
        super(GRID);
    }

    @Override
    protected void createFieldEditors() {
    }

    public void init(IWorkbench workbench) {
    }

}
