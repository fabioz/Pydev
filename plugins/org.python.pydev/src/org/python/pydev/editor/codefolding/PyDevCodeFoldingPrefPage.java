/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/**
 * @author Carl Robinson
 * @author fabioz
 * 
 * Created 12/09/07
 */
package org.python.pydev.editor.codefolding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.OverlayPreferenceStore;

/**
 * @author Carl Robinson
 *
 * Creates the preference page for code folding
 * Extends PreferencePage rather than FieldEditorPreferencePage
 * as it was easier to use some of the code in AbstractPydevPrefs
 * to create dependent checkboxes.
 */
public class PyDevCodeFoldingPrefPage extends PreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_CODE_FOLDING = "USE_CODE_FOLDING";

    public static final boolean DEFAULT_USE_CODE_FOLDING = true;

    public static final boolean DEFAULT_FOLD_IF = false;

    public static final String FOLD_IF = "FOLD_IF";

    public static final boolean DEFAULT_FOLD_WHILE = false;

    public static final String FOLD_WHILE = "FOLD_WHILE";

    public static final boolean DEFAULT_FOLD_IMPORTS = true;

    public static final String FOLD_IMPORTS = "FOLD_IMPORTS";

    public static final boolean DEFAULT_FOLD_COMMENTS = true;

    public static final String FOLD_COMMENTS = "FOLD_COMMENTS";

    public static final boolean DEFAULT_FOLD_STRINGS = true;

    public static final String FOLD_STRINGS = "FOLD_STRINGS";

    public static final boolean DEFAULT_FOLD_CLASSDEF = true;

    public static final String FOLD_CLASSDEF = "FOLD_CLASSDEF";

    public static final boolean DEFAULT_FOLD_FUNCTIONDEF = true;

    public static final String FOLD_FUNCTIONDEF = "FOLD_FUNCTIONDEF";

    public static final boolean DEFAULT_FOLD_FOR = false;

    public static final String FOLD_FOR = "FOLD_FOR";

    public static final boolean DEFAULT_FOLD_TRY = false;

    public static final String FOLD_TRY = "FOLD_TRY";

    public static final boolean DEFAULT_FOLD_WITH = false;

    public static final String FOLD_WITH = "FOLD_WITH";

    /**
     * 
     */
    public PyDevCodeFoldingPrefPage() {
        //super();
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("PyDev Code Folding Options");
        fOverlayStore = createOverlayStore();
    }

    /** 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        fOverlayStore.load();
        fOverlayStore.start();

        Control control = createPreferencePage(parent);

        initializeFields();

        //        Dialog.applyDialogFont(control);
        return control;
    }

    protected Control createPreferencePage(Composite parent) {
        Composite top = new Composite(parent, SWT.LEFT);

        // Sets the layout data for the top composite's 
        // place in its parent's layout.
        top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Sets the layout for the top composite's 
        // children to populate.
        top.setLayout(new GridLayout());

        Button master = addCheckBox(top, "Use Code Folding?  -  Will apply to new editors", USE_CODE_FOLDING, 0);

        Label listLabel = new Label(top, SWT.NONE);
        listLabel
                .setText("\nSelect the elements you would like PyDev \nto fold on.\n\nWill be applied when the document is saved");

        Button slaveImport = addCheckBox(top, "Fold Imports?", FOLD_IMPORTS, 0);

        Button slaveClass = addCheckBox(top, "Fold Class Definitions?", FOLD_CLASSDEF, 0);

        Button slaveFunc = addCheckBox(top, "Fold Function Definitions?", FOLD_FUNCTIONDEF, 0);

        Button slaveString = addCheckBox(top, "Fold Multi-line Strings?", FOLD_STRINGS, 0);

        Button slaveComment = addCheckBox(top, "Fold Comments?", FOLD_COMMENTS, 0);

        Button slaveFor = addCheckBox(top, "Fold FOR statments?", FOLD_FOR, 0);

        Button slaveIf = addCheckBox(top, "Fold IF statments?", FOLD_IF, 0);

        Button slaveTry = addCheckBox(top, "Fold TRY statments?", FOLD_TRY, 0);

        Button slaveWhile = addCheckBox(top, "Fold WHILE statments?", FOLD_WHILE, 0);

        Button slaveWith = addCheckBox(top, "Fold WITH statments?", FOLD_WITH, 0);

        createDependency(master, USE_CODE_FOLDING, slaveClass);
        createDependency(master, USE_CODE_FOLDING, slaveFunc);
        createDependency(master, USE_CODE_FOLDING, slaveImport);
        createDependency(master, USE_CODE_FOLDING, slaveFor);
        createDependency(master, USE_CODE_FOLDING, slaveIf);
        createDependency(master, USE_CODE_FOLDING, slaveTry);
        createDependency(master, USE_CODE_FOLDING, slaveWhile);
        createDependency(master, USE_CODE_FOLDING, slaveWith);
        createDependency(master, USE_CODE_FOLDING, slaveString);
        createDependency(master, USE_CODE_FOLDING, slaveComment);

        return top;

    }

    /*
     * @see PreferencePage#performOk()
     */
    public boolean performOk() {
        fOverlayStore.propagate();
        PydevPlugin.getDefault().savePluginPreferences();
        return true;
    }

    /*
     * @see PreferencePage#performDefaults()
     */
    protected void performDefaults() {

        fOverlayStore.loadDefaults();

        initializeFields();

        super.performDefaults();
    }

    protected void initializeFields() {

        Iterator<Button> e = fCheckBoxes.keySet().iterator();
        while (e.hasNext()) {
            Button b = (Button) e.next();
            String key = (String) fCheckBoxes.get(b);
            b.setSelection(fOverlayStore.getBoolean(key));
        }

        //        e= fTextFields.keySet().iterator();
        //        while (e.hasNext()) {
        //            Text t= (Text) e.next();
        //            String key= (String) fTextFields.get(t);
        //            t.setText(fOverlayStore.getString(key));
        //        }
        //        
        //        fFieldsInitialized= true;
        //        updateStatus(validatePositiveNumber("0")); 

        // Update slaves
        Iterator<SelectionListener> iter = fMasterSlaveListeners.iterator();
        while (iter.hasNext()) {
            SelectionListener listener = (SelectionListener) iter.next();
            listener.widgetSelected(null);
        }
    }

    protected OverlayPreferenceStore fOverlayStore;

    protected Map<Button, String> fCheckBoxes = new HashMap<Button, String>();

    protected SelectionListener fCheckBoxListener = new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
            Button button = (Button) e.widget;
            fOverlayStore.setValue((String) fCheckBoxes.get(button), button.getSelection());
        }
    };

    protected Button addCheckBox(Composite parent, String label, String key, int indentation) {
        Button checkBox = new Button(parent, SWT.CHECK);
        checkBox.setText(label);

        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalIndent = indentation;
        gd.horizontalSpan = 2;
        checkBox.setLayoutData(gd);
        checkBox.addSelectionListener(fCheckBoxListener);

        fCheckBoxes.put(checkBox, key);

        return checkBox;
    }

    protected java.util.List<SelectionListener> fMasterSlaveListeners = new ArrayList<SelectionListener>();

    protected void createDependency(final Button master, String masterKey, final Control slave) {
        indent(slave);

        boolean masterState = fOverlayStore.getBoolean(masterKey);
        slave.setEnabled(masterState);

        SelectionListener listener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                slave.setEnabled(master.getSelection());
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        };
        master.addSelectionListener(listener);
        fMasterSlaveListeners.add(listener);
    }

    protected static void indent(Control control) {
        GridData gridData = new GridData();
        gridData.horizontalIndent = 20;
        control.setLayoutData(gridData);
    }

    protected OverlayPreferenceStore createOverlayStore() {

        java.util.List<OverlayPreferenceStore.OverlayKey> overlayKeys = new ArrayList<OverlayPreferenceStore.OverlayKey>();

        //checkbox      
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_CODE_FOLDING));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_CLASSDEF));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_COMMENTS));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_FOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_FUNCTIONDEF));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_IF));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_IMPORTS));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_STRINGS));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_TRY));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_WHILE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_WITH));

        OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
        overlayKeys.toArray(keys);
        return new OverlayPreferenceStore(getPreferenceStore(), keys);
    }
}
