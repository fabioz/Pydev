package org.python.pydev.plugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;

public class PyCodeStylePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_LOCALS_AND_ATTRS_CAMELCASE = "USE_LOCALS_AND_ATTRS_CAMELCASE";
    
    public static final String USE_METHODS_FORMAT = "USE_METHODS_FORMAT";

    public static final boolean DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE = true;
    
    public static final int METHODS_FORMAT_CAMELCASE_FIRST_LOWER = 0;
    public static final int METHODS_FORMAT_CAMELCASE_FIRST_UPPER = 1;
    public static final int METHODS_FORMAT_UNDERSCORE_SEPARATED = 2;
    
    public static final int DEFAULT_USE_METHODS_FORMAT = METHODS_FORMAT_UNDERSCORE_SEPARATED;
    
    public static final String[][] LABEL_AND_VALUE = new String[][]{
        {"underscore_separated" , String.valueOf(METHODS_FORMAT_UNDERSCORE_SEPARATED)},
        {"CamelCase() with first upper"  , String.valueOf(METHODS_FORMAT_CAMELCASE_FIRST_LOWER)},
        {"camelCase() with first lower", String.valueOf(METHODS_FORMAT_CAMELCASE_FIRST_UPPER)},
    };

    private Label labelLocalsFormat;
    private Label labelMethodsFormat;

    private BooleanFieldEditor useCamelCase;
    private RadioGroupFieldEditor useMethodsFormat;

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
        
        useMethodsFormat = new RadioGroupFieldEditor(USE_METHODS_FORMAT, "Methods format", 1, LABEL_AND_VALUE, p, true);
        addField(useMethodsFormat);
        
        labelLocalsFormat = new Label(p, SWT.NONE);
        
        labelMethodsFormat = new Label(p, SWT.NONE);
        updateLabel(useLocalsAndAttrsCamelCase(), useMethodsCamelCase());
        
    }


    /**
     * Updates the label showing an example given the user suggestion.
     */
    private void updateLabel(boolean useCamelCase, int useMethodsFormat){
        if(useCamelCase){
            labelLocalsFormat.setText("Ctrl+1 for assign to variable will suggest: myValue = MyValue()    ");
        }else{
            labelLocalsFormat.setText("Ctrl+1 for assign to variable will suggest: my_value = MyValue()   ");
        }
        
        if(useMethodsFormat == METHODS_FORMAT_CAMELCASE_FIRST_UPPER){
            labelMethodsFormat.setText("Refactoring property methods in the format def MyMethod()    ");
            
        }else if(useMethodsFormat == METHODS_FORMAT_UNDERSCORE_SEPARATED){
            labelMethodsFormat.setText("Refactoring property methods in the format def my_method()   ");
            
        }else{
            //camelcase first lower is the default
            labelMethodsFormat.setText("Refactoring property methods in the format def myMethod()    ");
        }
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    public static int TESTING_METHOD_FORMAT = DEFAULT_USE_METHODS_FORMAT;
    public static int useMethodsCamelCase() {
        try{
            if(PydevPlugin.getDefault() == null){
                return TESTING_METHOD_FORMAT;
            }
            return Integer.parseInt(PydevPrefs.getPreferences().getString(USE_METHODS_FORMAT));
        }catch(NumberFormatException e){
            return DEFAULT_USE_METHODS_FORMAT;
        }
    }
    
    public static boolean useLocalsAndAttrsCamelCase() {
        return PydevPrefs.getPreferences().getBoolean(USE_LOCALS_AND_ATTRS_CAMELCASE);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event){
        super.propertyChange(event);
        
        if(useCamelCase.equals(event.getSource())){
            boolean newValue = (Boolean) event.getNewValue();
            updateLabel(newValue, useMethodsCamelCase());
            
        }else if(useMethodsFormat.equals(event.getSource())){
            String newValue = (String) event.getNewValue();
            int val;
            try{
                val = Integer.parseInt(newValue);
            }catch(NumberFormatException e){
                val = DEFAULT_USE_METHODS_FORMAT;
            }
            
            updateLabel(useLocalsAndAttrsCamelCase(), val);
        }
    }
}

