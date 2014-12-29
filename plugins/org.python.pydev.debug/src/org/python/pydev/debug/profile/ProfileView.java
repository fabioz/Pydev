package org.python.pydev.debug.profile;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.shared_ui.field_editors.BooleanFieldEditorCustom;
import org.python.pydev.shared_ui.field_editors.ComboFieldEditor;

public class ProfileView extends ViewPart {

    private BooleanFieldEditorCustom profileForNewLaunches;
    private FileFieldEditor pyvmmonitorUiLocation;
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
        editor.fillIntoGrid(parent, 3);
    }

    @Override
    public void createPartControl(Composite parent) {

        profileForNewLaunches = new BooleanFieldEditorCustom(PyProfilePreferences.ENABLE_PROFILING_FOR_NEW_LAUNCHES,
                "Enable profiling for new launches?", BooleanFieldEditorCustom.SEPARATE_LABEL, parent);
        addField(profileForNewLaunches, parent);

        pyvmmonitorUiLocation = new FileFieldEditor(PyProfilePreferences.PYVMMONITOR_UI_LOCATION,
                "pyvmmonitor-ui (executable) location", parent);
        addField(pyvmmonitorUiLocation, parent, PyProfilePreferences.getPermanentPreferenceStore());

        String[][] ENTRIES_AND_VALUES = new String[][] {

                { "Deterministic (profile/cProfile)",
                        Integer.toString(PyProfilePreferences.PROFILE_MODE_LSPROF) },

                { "Sampling (yappi)",
                        Integer.toString(PyProfilePreferences.PROFILE_MODE_YAPPI) },

                { "No profile on startup (only start connected)",
                        Integer.toString(PyProfilePreferences.PROFILE_MODE_NONE) },
        };
        ComboFieldEditor editor = new ComboFieldEditor(PyProfilePreferences.PROFILE_MODE,
                "Profile mode: ", ENTRIES_AND_VALUES, parent);
        addField(editor, parent, PyProfilePreferences.getPermanentPreferenceStore());

        GridLayout layout = GridLayoutFactory.swtDefaults().create();
        layout.numColumns = 3;
        parent.setLayout(layout);
    }

    @Override
    public void setFocus() {
        profileForNewLaunches.getCheckBox().setFocus();
    }

}
