package org.python.pydev.shared_ui.field_editors;

import static org.junit.Assert.assertTrue;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.json.eclipsesource.JsonArray;
import org.python.pydev.json.eclipsesource.JsonValue;

public class PathMappingsFieldEditor extends JsonFieldEditor {

    public PathMappingsFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, UNLIMITED, parent);
    }

    @Override
    protected boolean doCheckState() {
        return checkPathMappingsFormat(getTextControl().getText());
    }

    private boolean checkPathMappingsFormat(String str) {
        try {
            JsonArray array = JsonArray.readFrom(str);
            for (JsonValue value : array) {
                assertTrue(value.isObject());
            }
        } catch (Exception e) {
            setErrorMessage("Input not formatted as a Path Mappings entry.");
            return false;
        }

        return true;
    }
}
