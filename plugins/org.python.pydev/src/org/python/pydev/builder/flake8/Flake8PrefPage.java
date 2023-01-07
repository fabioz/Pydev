package org.python.pydev.builder.flake8;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
import org.python.pydev.shared_ui.field_editors.ArgsStringFieldEditor;
import org.python.pydev.shared_ui.field_editors.JsonFieldEditor;
import org.python.pydev.shared_ui.field_editors.RadioGroupFieldEditor;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;

import com.python.pydev.analysis.PyAnalysisScopedPreferences;
import com.python.pydev.analysis.flake8.Flake8CodesConfigHandler;
import com.python.pydev.analysis.flake8.Flake8Preferences;

public class Flake8PrefPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private Composite parent;
    private RadioGroupFieldEditor searchFlake8Location;
    private FileFieldEditor fileField;
    private List<FieldEditor> fields = new ArrayList<FieldEditor>(5);
    private JsonFieldEditor jsonFieldEditor;

    public static final int COLS = 4;

    public static final String[][] SEARCH_FLAKE8_LOCATION_OPTIONS = new String[][] {
            { "Search in interpreter", Flake8Preferences.LOCATION_SEARCH },
            { "Specify Location", Flake8Preferences.LOCATION_SPECIFY },
    };

    public Flake8PrefPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Flake8");
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    protected void initialize() {
        super.initialize();
        updateSelectFileEnablement(parent);
    }

    @Override
    protected void createFieldEditors() {
        parent = getFieldEditorParent();
        parent.setLayout(new GridLayout(1, false));

        addField(new BooleanFieldEditor(Flake8Preferences.USE_FLAKE8, "Use Flake8?", parent));
        addField(new BooleanFieldEditor(Flake8Preferences.FLAKE8_USE_CONSOLE, "Redirect Flake8 output to console?",
                parent));

        searchFlake8Location = new RadioGroupFieldEditor(Flake8Preferences.SEARCH_FLAKE8_LOCATION,
                "Flake8 to use", 2, SEARCH_FLAKE8_LOCATION_OPTIONS, parent);

        for (Button b : searchFlake8Location.getRadioButtons()) {
            b.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateSelectFileEnablement(parent);
                }
            });
        }

        addField(searchFlake8Location);
        fileField = new FileFieldEditor(Flake8Preferences.FLAKE8_FILE_LOCATION, "Location of the flake8 executable:",
                true, parent);
        addField(fileField);

        jsonFieldEditor = new JsonFieldEditor(Flake8Preferences.FLAKE8_CODES_CONFIG,
                WrapAndCaseUtils.wrap("Flake 8 severity map (unmatched entries will be marked as \"warning\")", 90),
                parent);
        jsonFieldEditor.setAdditionalJsonValidation((json) -> Flake8CodesConfigHandler.checkJsonFormat(json));
        addField(jsonFieldEditor);

        ArgsStringFieldEditor stringFieldEditor = new ArgsStringFieldEditor(Flake8Preferences.FLAKE8_ARGS,
                "Arguments to pass to the flake8 command.",
                parent);
        addField(stringFieldEditor);

        addField(new ScopedPreferencesFieldEditor(parent, PyAnalysisScopedPreferences.ANALYSIS_SCOPE, this));
    }

    @Override
    protected void addField(FieldEditor editor) {
        super.addField(editor);
        this.fields.add(editor);
    }

    @Override
    protected void adjustGridLayout() {
        parent = getFieldEditorParent();
        parent.setLayout(new GridLayout(1, false));
    }

    protected void updateSelectFileEnablement(Composite p) {
        fileField.setEnabled(Flake8Preferences.LOCATION_SPECIFY.equals(searchFlake8Location.getRadioValue()), p);
    }
}
