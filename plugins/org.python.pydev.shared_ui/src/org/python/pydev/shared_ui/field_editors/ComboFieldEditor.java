/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.field_editors;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.python.pydev.shared_core.log.Log;

/**
 * A field editor for a combo box that allows the drop-down selection of one of
 * a list of items.
 *
 * Note: copied to work on eclipse 3.2
 *
 * @since 3.3
 */
public class ComboFieldEditor extends FieldEditor {

    /**
     * The <code>Combo</code> widget.
     */
    private Combo fCombo;

    /**
     * The value (not the name) of the currently selected item in the Combo widget.
     */
    private String fValue;

    /**
     * The names (labels) and underlying values to populate the combo widget.  These should be
     * arranged as: { {name1, value1}, {name2, value2}, ...}
     */
    private String[][] fEntryNamesAndValues;

    /**
     * Create the combo box field editor.
     *
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param entryNamesAndValues the names (labels) and underlying values to populate the combo widget.  These should be
     * arranged as: { {name1, value1}, {name2, value2}, ...}
     * @param parent the parent composite
     */
    public ComboFieldEditor(String name, String labelText, String[][] entryNamesAndValues, Composite parent) {
        init(name, labelText);
        Assert.isTrue(checkArray(entryNamesAndValues));
        fEntryNamesAndValues = entryNamesAndValues;
        createControl(parent);
    }

    /**
     * Checks whether given <code>String[][]</code> is of "type"
     * <code>String[][2]</code>.
     *
     * @return <code>true</code> if it is ok, and <code>false</code> otherwise
     */
    private boolean checkArray(String[][] table) {
        if (table == null) {
            return false;
        }
        for (int i = 0; i < table.length; i++) {
            String[] array = table[i];
            if (array == null || array.length != 2) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void fillIntoGrid(Composite parent, int numColumns) {
        Assert.isTrue(parent.getLayout() instanceof GridLayout);
        doFillIntoGrid(parent, numColumns);
        adjustForNumColumns(numColumns);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        if (numColumns > 1) {
            Control control = getLabelControl();
            int left = numColumns;
            if (control != null) {
                ((GridData) control.getLayoutData()).horizontalSpan = 1;
                left = left - 1;
            }
            ((GridData) fCombo.getLayoutData()).horizontalSpan = left;
        } else {
            Control control = getLabelControl();
            if (control != null) {
                ((GridData) control.getLayoutData()).horizontalSpan = 1;
            }
            ((GridData) fCombo.getLayoutData()).horizontalSpan = 1;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#doFillIntoGrid(org.eclipse.swt.widgets.Composite, int)
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        int comboC = 1;
        if (numColumns > 1) {
            comboC = numColumns - 1;
        }
        Control control = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = 1;
        control.setLayoutData(gd);
        control = getComboBoxControl(parent);
        gd = new GridData();
        gd.horizontalSpan = comboC;
        gd.horizontalAlignment = GridData.FILL;
        control.setLayoutData(gd);
        control.setFont(parent.getFont());
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#doLoad()
     */
    @Override
    protected void doLoad() {
        updateComboForValue(getPreferenceStore().getString(getPreferenceName()));
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
     */
    @Override
    protected void doLoadDefault() {
        updateComboForValue(getPreferenceStore().getDefaultString(getPreferenceName()));
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#doStore()
     */
    @Override
    protected void doStore() {
        if (fValue == null) {
            getPreferenceStore().setToDefault(getPreferenceName());
            return;
        }
        getPreferenceStore().setValue(getPreferenceName(), fValue);
    }

    public String getComboValue() {
        return getValueForName(fCombo.getText());
    }

    public Combo getCombo() {
        return this.fCombo;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#getNumberOfControls()
     */
    @Override
    public int getNumberOfControls() {
        return 2;
    }

    /*
     * Lazily create and return the Combo control.
     */
    private Combo getComboBoxControl(Composite parent) {
        if (fCombo == null) {
            fCombo = new Combo(parent, SWT.READ_ONLY);
            fCombo.setFont(parent.getFont());
            for (int i = 0; i < fEntryNamesAndValues.length; i++) {
                fCombo.add(fEntryNamesAndValues[i][0], i);
            }

            fCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent evt) {
                    String oldValue = fValue;
                    String name = fCombo.getText();
                    fValue = getValueForName(name);
                    setPresentsDefaultValue(false);
                    fireValueChanged(VALUE, oldValue, fValue);
                }
            });
        }
        return fCombo;
    }

    /*
     * Given the name (label) of an entry, return the corresponding value.
     */
    private String getValueForName(String name) {
        for (int i = 0; i < fEntryNamesAndValues.length; i++) {
            String[] entry = fEntryNamesAndValues[i];
            if (name.equals(entry[0])) {
                return entry[1];
            }
        }

        Log.log("Unable to find entry for: " + name + " returning default.");
        return fEntryNamesAndValues[0][1];
    }

    /*
     * Set the name in the combo widget to match the specified value.
     */
    public void updateComboForValue(String value) {
        fValue = value;
        for (int i = 0; i < fEntryNamesAndValues.length; i++) {
            if (value.equals(fEntryNamesAndValues[i][1])) {
                fCombo.setText(fEntryNamesAndValues[i][0]);
                return;
            }
        }
        if (fEntryNamesAndValues.length > 0) {
            fValue = fEntryNamesAndValues[0][1];
            fCombo.setText(fEntryNamesAndValues[0][0]);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.FieldEditor#setEnabled(boolean,
     *      org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        super.setEnabled(enabled, parent);
        getComboBoxControl(parent).setEnabled(enabled);
    }
}
