package org.python.pydev.debug.newconsole.prefs;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.shared_ui.field_editors.MultiStringFieldEditor;

public class InteractiveConsoleInitialCommandsPreferencesPage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    public InteractiveConsoleInitialCommandsPreferencesPage() {
        super(FLAT);
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        addField(new MultiStringFieldEditor(PydevConsoleConstants.INITIAL_INTERPRETER_CMDS,
                "Initial interpreter commands.\n\nCan use variables from:\nRun/Debug > String Substitution", p));

        addField(new MultiStringFieldEditor(
                PydevConsoleConstants.DJANGO_INTERPRETER_CMDS,
                "Django interpreter commands.\n\nCan use variables from:\nRun/Debug > String Substitution\n\nUse ${DJANGO_SETTINGS_MODULE} to access\nthe project's Django settings module.",
                p));

    }

    public void init(IWorkbench workbench) {
        setDescription("PyDev interactive console initial commands.");
        setPreferenceStore(PydevDebugPlugin.getDefault().getPreferenceStore());
    }

}
