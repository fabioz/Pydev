package org.python.pydev.builder.flake8;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.field_editors.RadioGroupFieldEditor;
import org.python.pydev.utils.CustomizableFieldEditor;

import com.python.pydev.analysis.flake8.Flake8Preferences;

public class Flake8PrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private Composite parent;
    private RadioGroupFieldEditor searchFlake8Location;
    private FileFieldEditor fileField;
    private List<FieldEditor> fields = new ArrayList<FieldEditor>(5);

    public static final int COLS = 4;

    public static final String[][] LABEL_AND_VALUE = new String[][] {
            { "Error", String.valueOf(IMarker.SEVERITY_ERROR) },
            { "Warning", String.valueOf(IMarker.SEVERITY_WARNING) },
            { "Info", String.valueOf(IMarker.SEVERITY_INFO) },
            { "Ignore", String.valueOf(Flake8Preferences.SEVERITY_IGNORE) }, };

    public static final String[][] SEARCH_FLAKE8_LOCATION_OPTIONS = new String[][] {
            { "Search in interpreter", Flake8Preferences.LOCATION_SEARCH },
            { "Specify Location", Flake8Preferences.LOCATION_SPECIFY },
    };

    public Flake8PrefPage() {
        super(GRID);
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

        addField(new RadioGroupFieldEditor(Flake8Preferences.SEVERITY_ERROR, "ERROR Severity", COLS, LABEL_AND_VALUE,
                parent, true));

        addField(new RadioGroupFieldEditor(Flake8Preferences.SEVERITY_PYFLAKES, "PYFLAKES Severity", COLS,
                LABEL_AND_VALUE,
                parent, true));

        addField(new RadioGroupFieldEditor(Flake8Preferences.SEVERITY_WARNING, "WARNINGS Severity", COLS,
                LABEL_AND_VALUE, parent, true));

        addField(new RadioGroupFieldEditor(Flake8Preferences.SEVERITY_COMPLEXITY, "COMPLEXITY Severity", COLS,
                LABEL_AND_VALUE, parent, true));

        CustomizableFieldEditor stringFieldEditor = new CustomizableFieldEditor(Flake8Preferences.FLAKE8_ARGS,
                "Arguments to pass to the flake8 command.",
                parent);
        addField(stringFieldEditor);
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
