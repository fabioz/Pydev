/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.preferences.PydevPrefs;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeFormatterPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    //a, b, c
    public static final String USE_SPACE_AFTER_COMMA = "USE_SPACE_AFTER_COMMA";

    public static final boolean DEFAULT_USE_SPACE_AFTER_COMMA = true;

    
    //call( a )
    public static final String USE_SPACE_FOR_PARENTESIS = "USE_SPACE_FOR_PARENTESIS";

    public static final boolean DEFAULT_USE_SPACE_FOR_PARENTESIS = false;
    
    
    //call(a = 1)
    public static final String USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS = "USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS";
    
    public static final boolean DEFAULT_USE_ASSIGN_WITH_PACES_INSIDE_PARENTESIS = false;
    
    
    //operators =, !=, <, >, //, etc.
    public static final String USE_OPERATORS_WITH_SPACE = "USE_OPERATORS_WITH_SPACE";
    
    public static final boolean DEFAULT_USE_OPERATORS_WITH_SPACE = true;
    

    public PyCodeFormatterPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(USE_SPACE_AFTER_COMMA, "Use space after commas?", p));

        addField(new BooleanFieldEditor(USE_SPACE_FOR_PARENTESIS, "Use space before and after parenthesis?", p));
        
        addField(new BooleanFieldEditor(USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS, "Use space before and after assign for keyword arguments?", p));
        
        addField(new BooleanFieldEditor(USE_OPERATORS_WITH_SPACE, "Use space before and after operators? (+, -, /, *, //, **, etc.)", p));
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

    public static boolean useAssignWithSpacesInsideParenthesis() {
        return PydevPrefs.getPreferences().getBoolean(USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS);
    }

    public static boolean useOperatorsWithSpace() {
        return PydevPrefs.getPreferences().getBoolean(USE_OPERATORS_WITH_SPACE);
    }

}
