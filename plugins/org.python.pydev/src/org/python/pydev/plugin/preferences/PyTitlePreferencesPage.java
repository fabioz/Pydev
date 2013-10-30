/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;
import org.python.pydev.utils.TableComboFieldEditor;

public class PyTitlePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String TITLE_EDITOR_NAMES_UNIQUE = "TITLE_EDITOR_NAMES_UNIQUE";
    public static final boolean DEFAULT_TITLE_EDITOR_NAMES_UNIQUE = true;

    public static final String TITLE_EDITOR_SHOW_EXTENSION = "TITLE_EDITOR_SHOW_EXTENSION";
    public static final boolean DEFAULT_TITLE_EDITOR_SHOW_EXTENSION = false;

    public static final String TITLE_EDITOR_CUSTOM_INIT_ICON = "TITLE_EDITOR_CUSTOM_INIT_ICON";
    public static final boolean DEFAULT_TITLE_EDITOR_CUSTOM_INIT_ICON = true;

    public static final String TITLE_EDITOR_INIT_HANDLING = "TITLE_EDITOR_INIT_HANDLING";
    public static final String TITLE_EDITOR_INIT_HANDLING_IN_TITLE = "TITLE_EDITOR_INIT_HANDLING_IN_TITLE";
    public static final String TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE = "TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE";
    public static final String DEFAULT_TITLE_EDITOR_INIT_HANDLING = TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE;

    public static final String TITLE_EDITOR_DJANGO_MODULES_HANDLING = "TITLE_EDITOR_DJANGO_MODULES_HANDLING";
    public static final String TITLE_EDITOR_DJANGO_MODULES_DEFAULT_ICON = "TITLE_EDITOR_DJANGO_MODULES_DEFAULT_ICON";
    public static final String TITLE_EDITOR_DJANGO_MODULES_DECORATE = "TITLE_EDITOR_DJANGO_MODULES_DECORATE";
    public static final String TITLE_EDITOR_DJANGO_MODULES_SHOW_PARENT_AND_DECORATE = "TITLE_EDITOR_DJANGO_MODULES_SHOW_PARENT_AND_DECORATE";
    public static final String DEFAULT_TITLE_EDITOR_DJANGO_MODULES_HANDLING = TITLE_EDITOR_DJANGO_MODULES_SHOW_PARENT_AND_DECORATE;

    public static boolean isTitlePreferencesProperty(String property) {
        return PyTitlePreferencesPage.TITLE_EDITOR_INIT_HANDLING.equals(property)
                || PyTitlePreferencesPage.TITLE_EDITOR_NAMES_UNIQUE.equals(property)
                || PyTitlePreferencesPage.TITLE_EDITOR_SHOW_EXTENSION.equals(property)
                || PyTitlePreferencesPage.TITLE_EDITOR_CUSTOM_INIT_ICON.equals(property)
                || PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_HANDLING.equals(property);

    }

    public static boolean isTitlePreferencesIconRelatedProperty(String property) {
        return PyTitlePreferencesPage.TITLE_EDITOR_CUSTOM_INIT_ICON.equals(property)
                || PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_HANDLING.equals(property);
    }

    private BooleanFieldEditor editorNamesUnique;
    private BooleanFieldEditor changeInitIcon;
    private TableComboFieldEditor initHandlingFieldEditor;
    private BooleanFieldEditor titleShowExtension;
    private TableComboFieldEditor djangoHandling;

    public PyTitlePreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
    }

    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        //Unique names?
        editorNamesUnique = new BooleanFieldEditor(TITLE_EDITOR_NAMES_UNIQUE,
                "Editor name (considering icon) must be unique?", BooleanFieldEditor.SEPARATE_LABEL, p);
        addField(editorNamesUnique);

        titleShowExtension = new BooleanFieldEditor(TITLE_EDITOR_SHOW_EXTENSION, "Show file extension on tab?",
                BooleanFieldEditor.SEPARATE_LABEL, p);
        addField(titleShowExtension);

        //Should pydev change the init icon?
        changeInitIcon = new BooleanFieldEditor(TITLE_EDITOR_CUSTOM_INIT_ICON, "Use custom init icon?",
                BooleanFieldEditor.SEPARATE_LABEL, p);
        addField(changeInitIcon);

        Object[][] EDITOR__INIT__HANDLING_VALUES = {
                { "__init__.py should appear in title", TITLE_EDITOR_INIT_HANDLING_IN_TITLE, null },

                { "Show parent name in title", TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE, null },

        };

        initHandlingFieldEditor = new TableComboFieldEditor(TITLE_EDITOR_INIT_HANDLING, "__init__.py handling:",
                EDITOR__INIT__HANDLING_VALUES, p);
        addField(initHandlingFieldEditor);

        new LabelFieldEditor("UNUSED", "Django related configurations", p);
        new LabelFieldEditor("UNUSED", "", p);

        Object[][] EDITOR_DJANGO_MODULES_HANDLING_VALUES = {
                { "Show as regular module", TITLE_EDITOR_DJANGO_MODULES_DEFAULT_ICON, null },

                { "Show as regular module but using icon with module initial", TITLE_EDITOR_DJANGO_MODULES_DECORATE,
                        null },

                { "Show parent name in title and icon with module initial",
                        TITLE_EDITOR_DJANGO_MODULES_SHOW_PARENT_AND_DECORATE, null },

        };

        djangoHandling = new TableComboFieldEditor(TITLE_EDITOR_DJANGO_MODULES_HANDLING,
                "Django modules handling:\n(models.py, views.py, tests.py)", EDITOR_DJANGO_MODULES_HANDLING_VALUES, p);
        addField(djangoHandling);

    }

    public void dispose() {
        super.dispose();

    }

    public static boolean getEditorNamesUnique() {
        return PydevPrefs.getPreferences().getBoolean(TITLE_EDITOR_NAMES_UNIQUE);
    }

    /**
     * @return A constant defined in this class
     * EDITOR_TITLE_INIT_HANDLING_XXX
     * 
     * Note that clients using this methods can compare it with == with a constant in this
     * class (as it will return the actual constant and not what's set in the preferences).
     */
    public static String getInitHandling() {
        String initHandling = PydevPrefs.getPreferences().getString(TITLE_EDITOR_INIT_HANDLING);
        if (TITLE_EDITOR_INIT_HANDLING_IN_TITLE.equals(initHandling)) {
            return TITLE_EDITOR_INIT_HANDLING_IN_TITLE;
        }
        return TITLE_EDITOR_INIT_HANDLING_PARENT_IN_TITLE; //default
    }

    public static String getDjangoModulesHandling() {
        String djangoHandling = PydevPrefs.getPreferences().getString(TITLE_EDITOR_DJANGO_MODULES_HANDLING);
        if (TITLE_EDITOR_DJANGO_MODULES_DEFAULT_ICON.equals(djangoHandling)) {
            return TITLE_EDITOR_DJANGO_MODULES_DEFAULT_ICON;
        }
        if (TITLE_EDITOR_DJANGO_MODULES_DECORATE.equals(djangoHandling)) {
            return TITLE_EDITOR_DJANGO_MODULES_DECORATE;
        }
        return TITLE_EDITOR_DJANGO_MODULES_SHOW_PARENT_AND_DECORATE; //default
    }

    public static boolean useCustomInitIcon() {
        return PydevPrefs.getPreferences().getBoolean(TITLE_EDITOR_CUSTOM_INIT_ICON);
    }

    public static boolean getTitleShowExtension() {
        return PydevPrefs.getPreferences().getBoolean(TITLE_EDITOR_SHOW_EXTENSION);
    }

    public static Image getInitIcon() {
        ImageCache imageCache = PydevPlugin.getImageCache();
        if (useCustomInitIcon()) {
            return imageCache.get(UIConstants.CUSTOM_INIT_ICON);
        } else {
            return imageCache.get(UIConstants.PY_FILE_ICON); //default icon
        }
    }

    public static boolean isDjangoModuleToDecorate(String name) {
        if (name.startsWith("models.") || name.startsWith("tests.") || name.startsWith("views.")) {
            if (PythonPathHelper.isValidSourceFile(name)) {
                return true;
            }
        }
        return false;
    }

    public static Image getDjangoModuleIcon(String lastSegment) {
        return PydevPlugin.getImageCache().getStringDecorated(UIConstants.PY_FILE_CUSTOM_ICON,
                lastSegment.charAt(0) + "");
    }

}
