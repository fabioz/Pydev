package org.python.pydev.builder.mypy;

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
import com.python.pydev.analysis.mypy.MypyPreferences;

public class MypyPrefPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private Composite parent;
    private RadioGroupFieldEditor searchMypyLocation;
    private FileFieldEditor fileField;
    private List<FieldEditor> fields = new ArrayList<FieldEditor>(5);

    public static final String[][] SEARCH_MYPY_LOCATION_OPTIONS = new String[][] {
            { "Search in interpreter", MypyPreferences.LOCATION_SEARCH },
            { "Specify Location", MypyPreferences.LOCATION_SPECIFY },
    };

    public MypyPrefPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Mypy");
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

        addField(new BooleanFieldEditor(MypyPreferences.USE_MYPY, "Use Mypy?", parent));
        addField(new BooleanFieldEditor(MypyPreferences.MYPY_ADD_PROJECT_FOLDERS_TO_MYPYPATH,
                "Add project source folders to MYPYPATH?", parent));
        addField(new BooleanFieldEditor(MypyPreferences.MYPY_USE_CONSOLE, "Redirect Mypy output to console?", parent));

        searchMypyLocation = new RadioGroupFieldEditor(MypyPreferences.SEARCH_MYPY_LOCATION,
                "Mypy to use", 2, SEARCH_MYPY_LOCATION_OPTIONS, parent);

        for (Button b : searchMypyLocation.getRadioButtons()) {
            b.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateSelectFileEnablement(parent);
                }
            });
        }

        addField(searchMypyLocation);
        fileField = new FileFieldEditor(MypyPreferences.MYPY_FILE_LOCATION, "Location of the mypy executable:",
                true, parent);
        addField(fileField);

        ArgsStringFieldEditor stringFieldEditor = new ArgsStringFieldEditor(MypyPreferences.MYPY_ARGS,
                "Arguments to pass to the mypy command.",
                parent);
        addField(stringFieldEditor);
        addField(new LinkFieldEditor("MYPY_HELP",
                "View <a>http://www.pydev.org/manual_adv_mypy.html</a> for help.",
                parent,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Program.launch("http://www.pydev.org/manual_adv_mypy.html");
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
        fileField.setEnabled(MypyPreferences.LOCATION_SPECIFY.equals(searchMypyLocation.getRadioValue()), p);
    }
}
