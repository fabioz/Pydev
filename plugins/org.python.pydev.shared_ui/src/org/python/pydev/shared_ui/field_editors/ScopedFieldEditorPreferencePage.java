package org.python.pydev.shared_ui.field_editors;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Button;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.preferences.IScopedPreferences;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.Reflection;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.dialogs.DialogHelpers;

public abstract class ScopedFieldEditorPreferencePage extends FieldEditorPreferencePage {

    private List<FieldEditor> fields;

    public ScopedFieldEditorPreferencePage(int style) {
        super(style);
    }

    public ScopedFieldEditorPreferencePage(String title, int style) {
        super(title, style);
    }

    @Override
    protected void addField(FieldEditor editor) {
        super.addField(editor);
        if (fields == null) {
            fields = new ArrayList<FieldEditor>();
        }
        fields.add(editor);
    }

    public void saveToUserSettings(IScopedPreferences iScopedPreferences) {
        Map<String, Object> saveData = getFieldEditorsSaveData();
        if (saveData.size() > 0) {
            try {
                String message = iScopedPreferences.saveToUserSettings(saveData);
                DialogHelpers.openInfo("Results", message);
            } catch (Exception e) {
                Log.log(e);
                ErrorDialog.openError(EditorUtils.getShell(),
                        "Error: unable to save requested settings to user settings",
                        e.getMessage(),
                        SharedUiPlugin.makeErrorStatus(e, false));
            }
        } else {
            // This shouldn't happen
            DialogHelpers.openCritical("Error: No preferences to save",
                    "Error: No preferences to save (please report this as an error).");
        }
    }

    public void loadFromUserSettings(IScopedPreferences iScopedPreferences) {
        Map<String, Object> saveData = getFieldEditorsSaveData();
        if (saveData.size() > 0) {
            try {
                Tuple<Map<String, Object>, Set<String>> loadedFromUserSettings = iScopedPreferences
                        .loadFromUserSettings(saveData);

                updateFieldEditorsData(loadedFromUserSettings.o1);

                if (loadedFromUserSettings.o1.size() == 0) {
                    DialogHelpers.openInfo("No saved preferences",
                            "Unable to load any contents from the user settings.");

                } else if (loadedFromUserSettings.o2.size() > 0) {
                    DialogHelpers.openInfo("Partially loaded contents",
                            "Partially loaded contents. Did not find the keys below in the user settings:\n  "
                                    + StringUtils.join("\n  ", loadedFromUserSettings.o2));

                } else {
                    DialogHelpers.openInfo("Loaded contents", "Showing contents loaded from user settings.");

                }
            } catch (Exception e) {
                Log.log(e);
                ErrorDialog.openError(EditorUtils.getShell(),
                        "Error: unable to load requested settings from user settings",
                        e.getMessage(),
                        SharedUiPlugin.makeErrorStatus(e, false));
            }
        } else {
            // This shouldn't happen
            DialogHelpers.openCritical("Error: No preferences to load",
                    "Error: No preferences to load (please report this as an error).");
        }
    }

    public void loadFromProjectSettings(IScopedPreferences iScopedPreferences, IProject project) {
        Map<String, Object> saveData = getFieldEditorsSaveData();
        if (saveData.size() > 0) {
            try {
                Tuple<Map<String, Object>, Set<String>> loadedFromUserSettings = iScopedPreferences
                        .loadFromProjectSettings(saveData, project);

                updateFieldEditorsData(loadedFromUserSettings.o1);

                if (loadedFromUserSettings.o1.size() == 0) {
                    DialogHelpers.openInfo("No saved preferences",
                            "Unable to load any contents from the settings for the project: " + project.getName());

                } else if (loadedFromUserSettings.o2.size() > 0) {
                    DialogHelpers.openInfo("Partially loaded contents",
                            "Partially loaded contents. Did not find the keys below in the settings for the project "
                                    + project.getName() + ":\n  "
                                    + StringUtils.join("\n  ", loadedFromUserSettings.o2));

                } else {
                    DialogHelpers.openInfo("Loaded contents", "Showing contents loaded from settings in project: "
                            + project.getName());

                }
            } catch (Exception e) {
                Log.log(e);
                ErrorDialog.openError(EditorUtils.getShell(),
                        "Error: unable to load requested settings from settings in project: " + project.getName(),
                        e.getMessage(),
                        SharedUiPlugin.makeErrorStatus(e, false));
            }
        } else {
            // This shouldn't happen
            DialogHelpers.openCritical("Error: No preferences to load",
                    "Error: No preferences to load (please report this as an error).");
        }
    }

    private void updateFieldEditorsData(Map<String, Object> loadData) throws IllegalArgumentException,
            IllegalAccessException {
        if (fields != null) {
            Iterator<FieldEditor> e = fields.iterator();
            while (e.hasNext()) {
                FieldEditor pe = e.next();
                if (pe instanceof BooleanFieldEditor) {
                    BooleanFieldEditor booleanFieldEditor = (BooleanFieldEditor) pe;
                    String preferenceName = booleanFieldEditor.getPreferenceName();
                    Boolean value = (Boolean) loadData.get(preferenceName);
                    if (value == null) {
                        continue;
                    }

                    // Hack because the BooleanFieldEditor does not have a way to set the value in the view!
                    Field field = Reflection.getAttrFromClass(BooleanFieldEditor.class, "checkBox");
                    field.setAccessible(true);
                    Button checkbox = (Button) field.get(booleanFieldEditor);
                    checkbox.setSelection(value);

                } else if (pe instanceof IntegerFieldEditor) { //IntegerFieldEditor is a subclass of StringFieldEditor (so, must come before)
                    IntegerFieldEditor intFieldEditor = (IntegerFieldEditor) pe;
                    String preferenceName = intFieldEditor.getPreferenceName();
                    Object loaded = loadData.get(preferenceName);
                    if (loaded == null) {
                        continue;
                    }
                    if (loaded instanceof Integer) {
                        Integer value = (Integer) loaded;
                        intFieldEditor.setStringValue(Integer.toString(value));
                    } else {
                        intFieldEditor.setStringValue(loaded.toString());
                    }

                } else if (pe instanceof StringFieldEditor) { //IntegerFieldEditor is a subclass
                    StringFieldEditor stringFieldEditor = (StringFieldEditor) pe;
                    String preferenceName = stringFieldEditor.getPreferenceName();
                    String value = (String) loadData.get(preferenceName);
                    if (value == null) {
                        continue;
                    }
                    stringFieldEditor.setStringValue(value);

                } else if (pe instanceof ComboFieldEditor) {
                    ComboFieldEditor comboFieldEditor = (ComboFieldEditor) pe;
                    String preferenceName = comboFieldEditor.getPreferenceName();
                    String value = (String) loadData.get(preferenceName);
                    if (value == null) {
                        continue;
                    }
                    comboFieldEditor.updateComboForValue(value);

                } else if (pe instanceof RadioGroupFieldEditor) {
                    RadioGroupFieldEditor radioGroupFieldEditor = (RadioGroupFieldEditor) pe;
                    String preferenceName = radioGroupFieldEditor.getPreferenceName();
                    String value = (String) loadData.get(preferenceName);
                    if (value == null) {
                        continue;
                    }
                    radioGroupFieldEditor.updateRadioForValue(value);

                } else if (pe instanceof ScopedPreferencesFieldEditor || pe instanceof LinkFieldEditor
                        || pe instanceof LabelFieldEditor) {
                    // Ignore these ones

                } else {
                    Log.log("Unhandled field editor:" + pe);
                }
            }
        }

    }

    public Map<String, Object> getFieldEditorsSaveData() {
        Map<String, Object> saveData = new HashMap<>();
        if (fields != null) {
            Iterator<FieldEditor> e = fields.iterator();
            while (e.hasNext()) {
                FieldEditor pe = e.next();
                if (pe instanceof BooleanFieldEditor) {
                    BooleanFieldEditor booleanFieldEditor = (BooleanFieldEditor) pe;
                    boolean booleanValue = booleanFieldEditor.getBooleanValue();
                    String preferenceName = booleanFieldEditor.getPreferenceName();
                    saveData.put(preferenceName, booleanValue);

                } else if (pe instanceof IntegerFieldEditor) { //IntegerFieldEditor is a subclass of StringFieldEditor, so, must come first
                    IntegerFieldEditor intFieldEditor = (IntegerFieldEditor) pe;
                    String stringValue = intFieldEditor.getStringValue();
                    String preferenceName = intFieldEditor.getPreferenceName();
                    try {
                        saveData.put(preferenceName, Integer.parseInt(stringValue));
                    } catch (Exception e1) {
                        saveData.put(preferenceName, 0);
                    }

                } else if (pe instanceof StringFieldEditor) { //IntegerFieldEditor is a subclass
                    StringFieldEditor stringFieldEditor = (StringFieldEditor) pe;
                    String stringValue = stringFieldEditor.getStringValue();
                    String preferenceName = stringFieldEditor.getPreferenceName();
                    saveData.put(preferenceName, stringValue);

                } else if (pe instanceof ComboFieldEditor) {
                    ComboFieldEditor comboFieldEditor = (ComboFieldEditor) pe;
                    String stringValue = comboFieldEditor.getComboValue();
                    String preferenceName = comboFieldEditor.getPreferenceName();
                    saveData.put(preferenceName, stringValue);

                } else if (pe instanceof RadioGroupFieldEditor) {
                    RadioGroupFieldEditor radioGroupFieldEditor = (RadioGroupFieldEditor) pe;
                    String stringValue = radioGroupFieldEditor.getRadioValue();
                    String preferenceName = radioGroupFieldEditor.getPreferenceName();
                    saveData.put(preferenceName, stringValue);

                } else if (pe instanceof ScopedPreferencesFieldEditor || pe instanceof LinkFieldEditor
                        || pe instanceof LabelFieldEditor) {
                    // Ignore these ones

                } else {
                    Log.log("Unhandled field editor:" + pe);
                }
            }
        }
        return saveData;
    }

    public void saveToProjectSettings(IScopedPreferences iScopedPreferences, IProject[] projects) {
        Map<String, Object> saveData = getFieldEditorsSaveData();
        if (saveData.size() > 0) {
            try {
                String message = iScopedPreferences.saveToProjectSettings(saveData, projects);
                DialogHelpers.openInfo("Contents saved", message);
            } catch (Exception e) {
                Log.log(e);
                ErrorDialog.openError(EditorUtils.getShell(),
                        "Error: unable to save requested settings to user settings",
                        e.getMessage(),
                        SharedUiPlugin.makeErrorStatus(e, false));
            }
        } else {
            // This shouldn't happen
            DialogHelpers.openCritical("Error: No preferences to save",
                    "Error: No preferences to save (please report this as an error).");
        }

    }

    public void saveToWorkspace() {
        super.performApply();
    }

    public void loadFromWorkspace() {
        if (fields != null) {
            Iterator<FieldEditor> e = fields.iterator();
            while (e.hasNext()) {
                FieldEditor pe = e.next();
                pe.load();
            }
        }
    }

}
