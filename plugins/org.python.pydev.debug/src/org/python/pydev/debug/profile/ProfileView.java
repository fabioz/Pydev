package org.python.pydev.debug.profile;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.shared_ui.field_editors.BooleanFieldEditorCustom;
import org.python.pydev.shared_ui.field_editors.ComboFieldEditor;
import org.python.pydev.shared_ui.field_editors.FileFieldEditorCustom;

public class ProfileView extends ViewPart {

    private BooleanFieldEditorCustom profileForNewLaunches;
    private FileFieldEditorCustom pyvmmonitorUiLocation;
    private List<FieldEditor> fields = new ArrayList<FieldEditor>();

    protected void addField(final FieldEditor editor, Composite parent) {
        addField(editor, parent, PyProfilePreferences.getTemporaryPreferenceStore());
    }

    protected void addField(final FieldEditor editor, Composite parent, IPreferenceStore preferenceStore) {
        fields.add(editor);

        editor.setPreferenceStore(preferenceStore);
        editor.load();
        editor.setPropertyChangeListener(new IPropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                // Apply on any change!
                editor.store();
            }
        });
        if (editor instanceof BooleanFieldEditorCustom) {
            editor.fillIntoGrid(parent, 2);
        } else {
            editor.fillIntoGrid(parent, 1);
        }
    }

    @Override
    public void createPartControl(Composite parent) {

        Composite checkParent = new Composite(parent, SWT.NONE);
        checkParent.setLayoutData(GridDataFactory.fillDefaults().create());
        checkParent.setLayout(new GridLayout(2, false));

        profileForNewLaunches = new BooleanFieldEditorCustom(PyProfilePreferences.ENABLE_PROFILING_FOR_NEW_LAUNCHES,
                "Enable profiling for new launches?", BooleanFieldEditorCustom.SEPARATE_LABEL, checkParent);
        addField(profileForNewLaunches, checkParent);

        String[][] ENTRIES_AND_VALUES = new String[][] {

                { "Deterministic (profile)",
                        Integer.toString(PyProfilePreferences.PROFILE_MODE_LSPROF) },

                { "Sampling (yappi)",
                        Integer.toString(PyProfilePreferences.PROFILE_MODE_YAPPI) },

                { "Don't start profiling",
                        Integer.toString(PyProfilePreferences.PROFILE_MODE_NONE) },
        };
        ComboFieldEditor editor = new ComboFieldEditor(PyProfilePreferences.PROFILE_MODE,
                "Initial profile mode: ", ENTRIES_AND_VALUES, parent);
        addField(editor, parent, PyProfilePreferences.getPermanentPreferenceStore());

        Composite composite = new Composite(parent, SWT.NONE);
        GridData spacingLayoutData = new GridData();
        spacingLayoutData.heightHint = 8;
        composite.setLayoutData(spacingLayoutData);

        pyvmmonitorUiLocation = new FileFieldEditorCustom(PyProfilePreferences.PYVMMONITOR_UI_LOCATION,
                "pyvmmonitor-ui (executable) location", parent);
        addField(pyvmmonitorUiLocation, parent, PyProfilePreferences.getPermanentPreferenceStore());

        GridLayout layout = GridLayoutFactory.swtDefaults().create();
        layout.numColumns = 1;
        parent.setLayout(layout);
    }

    @Override
    public void setFocus() {
        profileForNewLaunches.getCheckBox().setFocus();
    }

}
