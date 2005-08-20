/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeFormatterPage extends FieldEditorPreferencePage 
implements IWorkbenchPreferencePage{

    public static final String USE_SPACE_AFTER_COMMA = "USE_SPACE_AFTER_COMMA";
    public static final boolean DEFAULT_USE_SPACE_AFTER_COMMA = true;
    
    public static final String USE_SPACE_FOR_PARENTESIS = "USE_SPACE_FOR_PARENTESIS";
    public static final boolean DEFAULT_USE_SPACE_FOR_PARENTESIS = true;
    
    /**
     * @param style
     */
    public PyCodeFormatterPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(
		        USE_SPACE_AFTER_COMMA, "Use space after commas?", p));

        addField(new BooleanFieldEditor(
		        USE_SPACE_FOR_PARENTESIS, "Use space before and after parenthesis?", p));
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    public static boolean useSpaceAfterComma() {
        return PydevPrefs.getPreferences().getBoolean(USE_SPACE_AFTER_COMMA);
    }

    public static boolean useSpaceForParentesis() {
        return PydevPrefs.getPreferences().getBoolean(USE_SPACE_FOR_PARENTESIS);
    }


}
