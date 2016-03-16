/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;

public class PyCodeStylePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_LOCALS_AND_ATTRS_CAMELCASE = "USE_LOCALS_AND_ATTRS_CAMELCASE";

    public static final String USE_METHODS_FORMAT = "USE_METHODS_FORMAT";

    public static final boolean DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE = false;

    public static final int METHODS_FORMAT_CAMELCASE_FIRST_LOWER = 0;
    public static final int METHODS_FORMAT_CAMELCASE_FIRST_UPPER = 1;
    public static final int METHODS_FORMAT_UNDERSCORE_SEPARATED = 2;

    public static final int DEFAULT_USE_METHODS_FORMAT = METHODS_FORMAT_UNDERSCORE_SEPARATED;

    public static final String[][] LABEL_AND_VALUE = new String[][] {
            { "underscore_separated", String.valueOf(METHODS_FORMAT_UNDERSCORE_SEPARATED) },
            { "CamelCase() with first upper", String.valueOf(METHODS_FORMAT_CAMELCASE_FIRST_UPPER) },
            { "camelCase() with first lower", String.valueOf(METHODS_FORMAT_CAMELCASE_FIRST_LOWER) }, };

    public static final String[][] LOCALS_LABEL_AND_VALUE = new String[][] {
            { "underscore_separated", String.valueOf(false) }, { "camelCase with first lower", String.valueOf(true) }, };

    private Label labelLocalsFormat;
    private Label labelMethodsFormat;

    private RadioGroupFieldEditor useLocalsAndAttrsCamelCase;
    private RadioGroupFieldEditor useMethodsFormat;

    public PyCodeStylePreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        useLocalsAndAttrsCamelCase = new RadioGroupFieldEditor(USE_LOCALS_AND_ATTRS_CAMELCASE,
                "Locals and attributes format (used for assign quick-assist)?", 1, LOCALS_LABEL_AND_VALUE, p, true);
        addField(useLocalsAndAttrsCamelCase);

        useMethodsFormat = new RadioGroupFieldEditor(USE_METHODS_FORMAT,
                "Methods format (used for generate properties refactoring)", 1, LABEL_AND_VALUE, p, true);
        addField(useMethodsFormat);

        labelLocalsFormat = new Label(p, SWT.NONE);

        labelMethodsFormat = new Label(p, SWT.NONE);
        updateLabelLocalsAndAttrs(useLocalsAndAttrsCamelCase());
        updateLabelMethods(useMethodsCamelCase());

    }

    /**
     * Updates the label showing an example given the user suggestion.
     */
    private void updateLabelMethods(int useMethodsFormat) {

        if (useMethodsFormat == METHODS_FORMAT_CAMELCASE_FIRST_UPPER) {
            labelMethodsFormat.setText("Refactoring property methods in the format def MyMethod()    ");

        } else if (useMethodsFormat == METHODS_FORMAT_UNDERSCORE_SEPARATED) {
            labelMethodsFormat.setText("Refactoring property methods in the format def my_method()   ");

        } else {
            //camelcase first lower is the default
            labelMethodsFormat.setText("Refactoring property methods in the format def myMethod()    ");
        }
    }

    private void updateLabelLocalsAndAttrs(boolean useCamelCase) {
        if (useCamelCase) {
            labelLocalsFormat.setText("Ctrl+1 for assign to variable will suggest: myValue = MyValue()    ");
        } else {
            labelLocalsFormat.setText("Ctrl+1 for assign to variable will suggest: my_value = MyValue()   ");
        }
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
    }

    public static int TESTING_METHOD_FORMAT = DEFAULT_USE_METHODS_FORMAT;

    public static int useMethodsCamelCase() {
        try {
            if (SharedCorePlugin.inTestMode()) {
                return TESTING_METHOD_FORMAT;
            }
            return Integer.parseInt(PydevPrefs.getPreferences().getString(USE_METHODS_FORMAT));
        } catch (NumberFormatException e) {
            return DEFAULT_USE_METHODS_FORMAT;
        }
    }

    public static boolean TESTING_METHOD_LOCALS_AND_ATTRS_CAMEL_CASE = DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE;

    public static boolean useLocalsAndAttrsCamelCase() {
        if (SharedCorePlugin.inTestMode()) {
            return TESTING_METHOD_LOCALS_AND_ATTRS_CAMEL_CASE;
        }
        return PydevPrefs.getPreferences().getBoolean(USE_LOCALS_AND_ATTRS_CAMELCASE);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);

        if (useLocalsAndAttrsCamelCase.equals(event.getSource())) {
            boolean newValue = Boolean.parseBoolean((String) event.getNewValue());
            updateLabelLocalsAndAttrs(newValue);

        } else if (useMethodsFormat.equals(event.getSource())) {
            int val;
            try {
                String newValue = (String) event.getNewValue();
                val = Integer.parseInt(newValue);
            } catch (NumberFormatException e) {
                val = DEFAULT_USE_METHODS_FORMAT;
            }

            updateLabelMethods(val);
        }
    }
}
