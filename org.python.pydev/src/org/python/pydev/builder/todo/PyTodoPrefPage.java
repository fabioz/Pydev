/*
 * Created on Oct 26, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.todo;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPrefs;

/**
 * @author Fabio Zadrozny
 */
public class PyTodoPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final String DEFAULT_PY_TODO_TAGS = "TODO: FIXME:";
    private static final String PY_TODO_TAGS = "PY_TODO_TAGS";

    public PyTodoPrefPage() {
        super(GRID);
        setDescription("Task tags");
    }

    public static void initializeDefaultPreferences(Preferences prefs) {
        prefs.setDefault(PY_TODO_TAGS, DEFAULT_PY_TODO_TAGS);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        addField(new StringFieldEditor(PY_TODO_TAGS, "Todo tags (separated by spaces)", p));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
        
    }

    public static List getTodoTags(){
        String string = PydevPrefs.getPreferences().getString(PY_TODO_TAGS);
        String[] strings = string.split(" ");
        return Arrays.asList(strings);
    }
}
