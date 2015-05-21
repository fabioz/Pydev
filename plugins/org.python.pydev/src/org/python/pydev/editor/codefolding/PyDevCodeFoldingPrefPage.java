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

    public static final String INITIALLY_FOLD_COMMENTS = "INITIALLY_COLLAPSE_COMMENTS";

    public static final boolean DEFAULT_INITIALLY_FOLD_COMMENTS = false;

    public static final String INITIALLY_FOLD_IF = "INITIALLY_FOLD_IF";

    public static final boolean DEFAULT_INITIALLY_FOLD_IF = false;

    public static final String INITIALLY_FOLD_WHILE = "INITIALLY_FOLD_WHILE";

    public static final boolean DEFAULT_INITIALLY_FOLD_WHILE = false;

    public static final String INITIALLY_FOLD_CLASSDEF = "INITIALLY_FOLD_CLASSDEF";

    public static final boolean DEFAULT_INITIALLY_FOLD_CLASSDEF = false;

    public static final String INITIALLY_FOLD_FUNCTIONDEF = "INITIALLY_FOLD_FUNCTIONDEF";

    public static final boolean DEFAULT_INITIALLY_FOLD_FUNCTIONDEF = false;

    public static final String INITIALLY_FOLD_STRINGS = "INITIALLY_FOLD_STRINGS";

    public static final boolean DEFAULT_INITIALLY_FOLD_STRINGS = false;

    public static final String INITIALLY_FOLD_WITH = "INITIALLY_FOLD_WITH";

    public static final boolean DEFAULT_INITIALLY_FOLD_WITH = false;

    public static final String INITIALLY_FOLD_TRY = "INITIALLY_FOLD_TRY";

    public static final boolean DEFAULT_INITIALLY_FOLD_TRY = false;

    public static final String INITIALLY_FOLD_IMPORTS = "INITIALLY_FOLD_IMPORTS";

    public static final boolean DEFAULT_INITIALLY_FOLD_IMPORTS = false;

    public static final String INITIALLY_FOLD_FOR = "INITIALLY_FOLD_FOR";

    public static final boolean DEFAULT_INITIALLY_FOLD_FOR = false;

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
                .setText(
                        "\nSelect the elements you would like PyDev \nto fold on.\n\nWill be applied when the document is saved");

        /*[[[cog
        import cog
        template = '''Button slave%(titled)s = addCheckBox(top, "Fold %(caption)ss?", %(constant)s, 0);
        Button slaveInitialCollapse%(titled)s = addCheckBox(top, "Initially Fold %(caption)ss?", INITIALLY_%(constant)s, 0);
        createDependency(new Button[] { master, slave%(titled)s }, slaveInitialCollapse%(titled)s, USE_CODE_FOLDING, %(constant)s);
        '''
        import folding_entries
        for constant, caption in zip(folding_entries.FOLDING_ENTRIES, folding_entries.FOLDING_CAPTIONS):
            titled = constant.title().replace('_', '');
            cog.outl(template % dict(titled=titled, constant=constant, caption=caption))
            
        ]]]*/
        Button slaveFoldImports = addCheckBox(top, "Fold Imports?", FOLD_IMPORTS, 0);
        Button slaveInitialCollapseFoldImports = addCheckBox(top, "Initially Fold Imports?", INITIALLY_FOLD_IMPORTS, 0);
        createDependency(new Button[] { master, slaveFoldImports }, slaveInitialCollapseFoldImports, USE_CODE_FOLDING, FOLD_IMPORTS);

        Button slaveFoldClassdef = addCheckBox(top, "Fold Class Definitions?", FOLD_CLASSDEF, 0);
        Button slaveInitialCollapseFoldClassdef = addCheckBox(top, "Initially Fold Class Definitions?", INITIALLY_FOLD_CLASSDEF, 0);
        createDependency(new Button[] { master, slaveFoldClassdef }, slaveInitialCollapseFoldClassdef, USE_CODE_FOLDING, FOLD_CLASSDEF);

        Button slaveFoldFunctiondef = addCheckBox(top, "Fold Function Definitions?", FOLD_FUNCTIONDEF, 0);
        Button slaveInitialCollapseFoldFunctiondef = addCheckBox(top, "Initially Fold Function Definitions?", INITIALLY_FOLD_FUNCTIONDEF, 0);
        createDependency(new Button[] { master, slaveFoldFunctiondef }, slaveInitialCollapseFoldFunctiondef, USE_CODE_FOLDING, FOLD_FUNCTIONDEF);

        Button slaveFoldComments = addCheckBox(top, "Fold Comments?", FOLD_COMMENTS, 0);
        Button slaveInitialCollapseFoldComments = addCheckBox(top, "Initially Fold Comments?", INITIALLY_FOLD_COMMENTS, 0);
        createDependency(new Button[] { master, slaveFoldComments }, slaveInitialCollapseFoldComments, USE_CODE_FOLDING, FOLD_COMMENTS);

        Button slaveFoldStrings = addCheckBox(top, "Fold Strings?", FOLD_STRINGS, 0);
        Button slaveInitialCollapseFoldStrings = addCheckBox(top, "Initially Fold Strings?", INITIALLY_FOLD_STRINGS, 0);
        createDependency(new Button[] { master, slaveFoldStrings }, slaveInitialCollapseFoldStrings, USE_CODE_FOLDING, FOLD_STRINGS);

        Button slaveFoldIf = addCheckBox(top, "Fold If statements?", FOLD_IF, 0);
        Button slaveInitialCollapseFoldIf = addCheckBox(top, "Initially Fold If statements?", INITIALLY_FOLD_IF, 0);
        createDependency(new Button[] { master, slaveFoldIf }, slaveInitialCollapseFoldIf, USE_CODE_FOLDING, FOLD_IF);

        Button slaveFoldWhile = addCheckBox(top, "Fold While statements?", FOLD_WHILE, 0);
        Button slaveInitialCollapseFoldWhile = addCheckBox(top, "Initially Fold While statements?", INITIALLY_FOLD_WHILE, 0);
        createDependency(new Button[] { master, slaveFoldWhile }, slaveInitialCollapseFoldWhile, USE_CODE_FOLDING, FOLD_WHILE);

        Button slaveFoldWith = addCheckBox(top, "Fold With statements?", FOLD_WITH, 0);
        Button slaveInitialCollapseFoldWith = addCheckBox(top, "Initially Fold With statements?", INITIALLY_FOLD_WITH, 0);
        createDependency(new Button[] { master, slaveFoldWith }, slaveInitialCollapseFoldWith, USE_CODE_FOLDING, FOLD_WITH);

        Button slaveFoldTry = addCheckBox(top, "Fold Try statements?", FOLD_TRY, 0);
        Button slaveInitialCollapseFoldTry = addCheckBox(top, "Initially Fold Try statements?", INITIALLY_FOLD_TRY, 0);
        createDependency(new Button[] { master, slaveFoldTry }, slaveInitialCollapseFoldTry, USE_CODE_FOLDING, FOLD_TRY);

        Button slaveFoldFor = addCheckBox(top, "Fold For statements?", FOLD_FOR, 0);
        Button slaveInitialCollapseFoldFor = addCheckBox(top, "Initially Fold For statements?", INITIALLY_FOLD_FOR, 0);
        createDependency(new Button[] { master, slaveFoldFor }, slaveInitialCollapseFoldFor, USE_CODE_FOLDING, FOLD_FOR);

        //[[[end]]]

        return top;

    }

    /*
     * @see PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        fOverlayStore.propagate();
        PydevPlugin.getDefault().savePluginPreferences();
        return true;
    }

    /*
     * @see PreferencePage#performDefaults()
     */
    @Override
    protected void performDefaults() {

        fOverlayStore.loadDefaults();

        initializeFields();

        super.performDefaults();
    }

    protected void initializeFields() {

        Iterator<Button> e = fCheckBoxes.keySet().iterator();
        while (e.hasNext()) {
            Button b = e.next();
            String key = fCheckBoxes.get(b);
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
            SelectionListener listener = iter.next();
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
            fOverlayStore.setValue(fCheckBoxes.get(button), button.getSelection());
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

    protected void createDependency(final Button[] masterControls, final Control slave, String... masterKeys) {
        indent(slave, 20 * masterKeys.length);

        boolean masterState = true;
        for (String string : masterKeys) {
            masterState &= fOverlayStore.getBoolean(string);
        }
        slave.setEnabled(masterState);

        SelectionListener listener = new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                boolean enabled = true;
                for (Button master : masterControls) {
                    enabled &= master.getSelection();
                }
                slave.setEnabled(enabled);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        };
        for (Button master : masterControls) {
            master.addSelectionListener(listener);
        }
        fMasterSlaveListeners.add(listener);
    }

    protected static void indent(Control control, int horizontalIndent) {
        GridData gridData = new GridData();
        gridData.horizontalIndent = horizontalIndent;
        control.setLayoutData(gridData);
    }

    protected OverlayPreferenceStore createOverlayStore() {

        java.util.List<OverlayPreferenceStore.OverlayKey> overlayKeys = new ArrayList<OverlayPreferenceStore.OverlayKey>();
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, USE_CODE_FOLDING));

        //checkbox      
        /*[[[cog
        import cog
        template = '''overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, %s));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_%s));
        '''
        import folding_entries
        for s in folding_entries.FOLDING_ENTRIES:
            cog.outl(template % (s, s))
            
        ]]]*/
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_IMPORTS));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_IMPORTS));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_CLASSDEF));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_CLASSDEF));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_FUNCTIONDEF));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_FUNCTIONDEF));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_COMMENTS));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_COMMENTS));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_STRINGS));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_STRINGS));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_IF));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_IF));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_WHILE));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_WHILE));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_WITH));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_WITH));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_TRY));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_TRY));

        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, FOLD_FOR));
        overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, INITIALLY_FOLD_FOR));

        //[[[end]]]

        OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
        overlayKeys.toArray(keys);
        return new OverlayPreferenceStore(getPreferenceStore(), keys);
    }
}
