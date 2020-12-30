package org.python.pydev.debug.ui;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.json.eclipsesource.JsonArray;
import org.python.pydev.json.eclipsesource.JsonObject;
import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PyDevEditorPreferences;
import org.python.pydev.shared_ui.field_editors.JsonFieldEditor;

/**
 * Preferences for the locations that should be translated -- used when the debugger is not able
 * to find some path aa the client, so, the user is asked for the location and the answer is
 * kept in the preferences in the format:
 * 
 * path asked, new path -- means that a request for the "path asked" should return the "new path"
 * path asked, DONTASK -- means that if some request for that file was asked it should silently ignore it
 */
public class PathMappingsPrefsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private JsonFieldEditor jsonFieldEditor;

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
        jsonFieldEditor = new JsonFieldEditor(PyDevEditorPreferences.PATHS_FROM_ECLIPSE_TO_PYTHON,
                "Path Mappings JSON input", p);

        jsonFieldEditor.setAdditionalJsonValidation((json) -> checkPathMappingsFormat(json));

        addField(jsonFieldEditor);
    }

    private Optional<String> checkPathMappingsFormat(JsonValue json) {
        if (!json.isArray()) {
            return Optional.of("Path Mappings JSON must be an array.");
        }
        JsonArray array = json.asArray();
        if (array.size() == 0) {
            return Optional.of("Empty array.");
        }

        int i = 0;
        for (JsonValue value : array) {
            if (!value.isObject()) {
                return Optional.of("Only objects are accepted.");
            }

            i++;
            String objectId = "Object " + i;
            JsonObject object = value.asObject();
            if (object.size() != 2) {
                return Optional.of(objectId + " does not have a valid size.");
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