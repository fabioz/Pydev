package org.python.pydev.builder.pyright;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.field_editors.ArgsStringFieldEditor;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.field_editors.RadioGroupFieldEditor;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;

import com.python.pydev.analysis.PyAnalysisScopedPreferences;
import com.python.pydev.analysis.pyright.PyrightPreferences;

public class PyrightPrefPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private Composite parent;
    private RadioGroupFieldEditor searchPyrightLocation;
    private FileFieldEditor fileField;
    private List<FieldEditor> fields = new ArrayList<FieldEditor>(5);

    public static final String[][] SEARCH_PYRIGHT_LOCATION_OPTIONS = new String[][] {
            { "Search in interpreter", PyrightPreferences.LOCATION_SEARCH },
            { "Specify Location", PyrightPreferences.LOCATION_SPECIFY },
    };

    public PyrightPrefPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Pyright");
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

        addField(new BooleanFieldEditor(PyrightPreferences.USE_PYRIGHT, "Use Pyright?", parent));
        addField(new BooleanFieldEditor(PyrightPreferences.PYRIGHT_USE_CONSOLE, "Redirect Pyright output to console?", parent));

        searchPyrightLocation = new RadioGroupFieldEditor(PyrightPreferences.SEARCH_PYRIGHT_LOCATION,
                "Pyright to use", 2, SEARCH_PYRIGHT_LOCATION_OPTIONS, parent);

        for (Button b : searchPyrightLocation.getRadioButtons()) {
            b.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateSelectFileEnablement(parent);
                }
            });
        }

        addField(searchPyrightLocation);
        fileField = new FileFieldEditor(PyrightPreferences.PYRIGHT_FILE_LOCATION, "Location of the pyright executable:",
                true, parent);
        addField(fileField);

        ArgsStringFieldEditor stringFieldEditor = new ArgsStringFieldEditor(PyrightPreferences.PYRIGHT_ARGS,
                "Arguments to pass to the pyright command.",
                parent);
        addField(stringFieldEditor);
        addField(new LinkFieldEditor("PYRIGHT_HELP",
                "View <a>http://www.pydev.org/manual_adv_pyright.html</a> for help.",
                parent,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Program.launch("http://www.pydev.org/manual_adv_pyright.html");
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                }));
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
        fileField.setEnabled(PyrightPreferences.LOCATION_SPECIFY.equals(searchPyrightLocation.getRadioValue()), p);
    }
}
