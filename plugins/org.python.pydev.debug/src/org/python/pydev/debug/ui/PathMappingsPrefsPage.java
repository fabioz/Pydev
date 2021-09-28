package org.python.pydev.debug.ui;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.json.eclipsesource.JsonArray;
import org.python.pydev.json.eclipsesource.JsonObject;
import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PyDevEditorPreferences;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
import org.python.pydev.shared_ui.field_editors.ButtonFieldEditor;
import org.python.pydev.shared_ui.field_editors.JsonFieldEditor;

public class PathMappingsPrefsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private JsonFieldEditor jsonFieldEditor;

    private final String pathMappingsListTemplate = "[\n" +
            "    {\n" +
            "      \"localRoot\": \"c:/temp\", \n" +
            "      \"remoteRoot\": \"/usr/temp\"\n" +
            "    }\n" +
            "]";

    private final String pathMappingsObjectTemplate = ",\n"
            + "    {\n" +
            "      \"localRoot\": \"c:/src\", \n" +
            "      \"remoteRoot\": \"/usr/src\"\n" +
            "    }\n"
            + "]";

    private ButtonFieldEditor btFieldEditor;

    /**
     * Initializer sets the preference store
     */
    public PathMappingsPrefsPage() {
        super("Path Mapping", GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    /**
     * Creates the editors
     */
    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        jsonFieldEditor = new JsonFieldEditor(PyDevEditorPreferences.PATH_MAPPINGS,
                WrapAndCaseUtils.wrap(
                        "Path Mappings JSON input.\n\nWhen the debugger is running in a different machine/VM/Docker, it's possible to set how paths should be translated.",
                        90) + "\n\n"
                        + WrapAndCaseUtils.wrap(
                                "The field below should be filled with JSON (with a list of objects with \"localRoot\" and \"remoteRoot\"). "
                                        + "Use the \"Add path mapping template entry.\" button below to add one entry.",
                                90),
                p);
        jsonFieldEditor.setAdditionalJsonValidation((json) -> checkPathMappingsFormat(json));
        addField(jsonFieldEditor);

        btFieldEditor = new ButtonFieldEditor("__UNUSED__", "Add path mapping template entry.", p,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        addTemplateButtonClick();
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        addField(btFieldEditor);

        addField(new BooleanFieldEditor(PyDevEditorPreferences.DEBUG_PATH_MAPPINGS,
                "Debug path translation? i.e.: Prints the translation to stderr on the server.", p));
    }

    private void addTemplateButtonClick() {
        if (jsonFieldEditor != null) {
            StyledText textField = jsonFieldEditor.getTextControl(getFieldEditorParent());
            if (textField != null && !textField.isDisposed()) {
                FastStringBuffer contentBuffer = new FastStringBuffer();
                contentBuffer.append(textField.getText()).trim();
                if (contentBuffer.isEmpty()) {
                    textField.setText(pathMappingsListTemplate);
                } else if (jsonFieldEditor.isValidToApply()) {
                    contentBuffer.deleteLast();
                    contentBuffer.trim().append(pathMappingsObjectTemplate);
                    textField.setText(contentBuffer.toString());
                }
            }
        }
    }

    @Override
    protected void updateApplyButton() {
        super.updateApplyButton();
        btFieldEditor.getButtonControl(getFieldEditorParent()).setEnabled(isValid());
    }

    private Optional<String> checkPathMappingsFormat(JsonValue json) {
        if (!json.isArray()) {
            return Optional.of("Path Mappings JSON must be an array.");
        }
        JsonArray array = json.asArray();
        int i = 0;
        for (JsonValue value : array) {
            if (!value.isObject()) {
                return Optional
                        .of("Only objects with \"localRoot\" and \"remoteRoot\" are accepted as path mapping entries.");
            }

            i++;
            String objectId = "Path mapping entry: " + i;
            JsonObject object = value.asObject();
            if (object.size() != 2) {
                return Optional.of(objectId + " is expected to contain \"localRoot\" and \"remoteRoot\" keys.");
            }

            List<String> objectNames = object.names();
            if (!objectNames.contains("localRoot")) {
                return Optional.of(objectId + " does not contain \"localRoot\" key.");
            }
            if (!objectNames.contains("remoteRoot")) {
                return Optional.of(objectId + " does not contain \"remoteRoot\" key.");
            }

            if (!object.get("localRoot").isString()) {
                return Optional.of(objectId + " \"localRoot\" value is not a string.");
            }
            if (!object.get("remoteRoot").isString()) {
                return Optional.of(objectId + " \"remoteRoot\" value is not a string.");
            }
        }
        // no errors
        return Optional.empty();
    }

    @Override
    protected void performApply() {
        if (jsonFieldEditor.isValidToApply()) {
            super.performApply();
        }
    }

}
