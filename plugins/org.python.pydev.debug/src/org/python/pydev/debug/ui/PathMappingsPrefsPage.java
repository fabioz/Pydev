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
        JsonFieldEditor jsonFieldEditor = new JsonFieldEditor(PyDevEditorPreferences.PATHS_FROM_ECLIPSE_TO_PYTHON,
                "Path Mappings JSON input", p);

        jsonFieldEditor.setAdditionalJsonValidation((json) -> {
            // Validate JSON and return error
            if (!checkPathMappingsFormat(json)) {
                return Optional.of("Not a valid Path Mappings input.");
            }
            // No error
            return Optional.empty();
        });
        addField(jsonFieldEditor);
    }

    private boolean checkPathMappingsFormat(JsonValue json) {
        if (!json.isArray()) {
            return false;
        }
        JsonArray array = json.asArray();
        if (array.size() == 0) {
            return false;
        }
        for (JsonValue value : array) {
            if (!value.isObject()) {
                return false;
            }
            JsonObject object = value.asObject();
            if (object.size() != 2) {
                return false;
            }
            List<String> objectNames = object.names();
            if (!objectNames.contains("localRoot") || !objectNames.contains("remoteRoot")) {
                return false;
            }

            if (!object.get("localRoot").isString() || !object.get("remoteRoot").isString()) {
                return false;
            }
        }
        return true;
    }

}
