package org.python.pydev.plugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;

public class PyCodeStylePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_LOCALS_AND_ATTRS_CAMELCASE = "USE_LOCALS_AND_ATTRS_CAMELCASE";

    public static final boolean DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE = true;

    private Label label;

    private BooleanFieldEditor useCamelCase;

    public PyCodeStylePreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();
        

        useCamelCase = new BooleanFieldEditor(USE_LOCALS_AND_ATTRS_CAMELCASE, "Use locals and attrs in camel case (used for assign quick-assist)?", p);
        addField(useCamelCase);
        
        label = new Label(p, SWT.NONE);
        updateLabel(useLocalsAndAttrsCamelCase());
        
    }

    /**
     * Updates the label showing an example given the user suggestion.
     */
    private void updateLabel(boolean useCamelCase){
        if(useCamelCase){
            label.setText("Ctrl+1 for assign to variable will suggest: myValue = MyValue()    ");
        }else{
            label.setText("Ctrl+1 for assign to variable will suggest: my_value = MyValue()   ");
        }
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    public static boolean useLocalsAndAttrsCamelCase() {
        return PydevPrefs.getPreferences().getBoolean(USE_LOCALS_AND_ATTRS_CAMELCASE);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event){
        super.propertyChange(event);
        if(useCamelCase.equals(event.getSource())){
            boolean newValue = (Boolean) event.getNewValue();
            updateLabel(newValue);
        }
    }
}

