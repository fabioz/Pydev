/******************************************************************************
* Copyright (C) 2013  André Berg and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     André Berg <andre.bergmedia@googlemail.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>           - ongoing maintenance
******************************************************************************/
package org.python.pydev.editor.saveactions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.core.SystemUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.preferences.PyScopedPreferences;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;
import org.python.pydev.shared_ui.tooltips.presenter.AbstractTooltipInformationPresenter;
import org.python.pydev.shared_ui.tooltips.presenter.ToolTipPresenterHandler;

/**
 * Preference page for Pydev editor {@code Save Actions}.
 * Save actions are actions performed on file buffers whenever
 * a file resource is saved.
 *
 * @author André Berg
 * @version 0.1
 */
public class PydevSaveActionsPrefPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private class PydevSaveActionsPageLinkListener implements SelectionListener {

        public PydevSaveActionsPageLinkListener() {
        }

        public void widgetSelected(SelectionEvent e) {
            try {
                URL url = new URL("http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html");
                SystemUtils.openWebpageInEclipse(url, "SimpleDateFormat Java Docs");
            } catch (MalformedURLException e1) {
                Log.log(e1);
            }
        }

        public void widgetDefaultSelected(SelectionEvent e) {
        }

    }

    private StringFieldEditor dateFormatEditor;
    private PydevDateFieldNameEditor fieldNameEditor;
    private BooleanFieldEditor enableDateFieldActionEditor;
    private LinkFieldEditor dateFormatHelpLinkEditor;

    private static final String enableDateFieldActionEditorTooltipFormat = "" +
            "Parses the file being saved for a module level\n" +
            "field with name and value as defined by the custom\n" +
            "name and date format below and updates it to the\n" +
            "current date.";

    private ToolTipPresenterHandler tooltipPresenter;
    private BooleanFieldEditor sortImportsOnSave;

    public PydevSaveActionsPrefPage() {
        super(GRID);
        final IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        setDescription("Save actions are run whenever a file is saved.\n");
        setPreferenceStore(store);
    }

    public static final String SAVE_ACTIONS_ONLY_ON_WORKSPACE_FILES = "SAVE_ACTIONS_ONLY_ON_WORKSPACE_FILES";
    public static final boolean DEFAULT_SAVE_ACTIONS_ONLY_ON_WORKSPACE_FILES = true;

    public static final String FORMAT_BEFORE_SAVING = "FORMAT_BEFORE_SAVING";
    public static final boolean DEFAULT_FORMAT_BEFORE_SAVING = false;

    public static final String ENABLE_DATE_FIELD_ACTION = "ENABLE_DATE_FIELD_ACTION";
    public static final boolean DEFAULT_ENABLE_DATE_FIELD_ACTION = false;

    public static final String DATE_FIELD_FORMAT = "DATE_FIELD_FORMAT";
    public static final String DEFAULT_DATE_FIELD_FORMAT = "yyyy-MM-dd";

    public static final String DATE_FIELD_NAME = "DATE_FIELD_NAME";
    public static final String DEFAULT_DATE_FIELD_NAME = "__updated__";

    public static final String SORT_IMPORTS_ON_SAVE = "SORT_IMPORTS_ON_SAVE";
    public static final boolean DEFAULT_SORT_IMPORTS_ON_SAVE = false;

    @Override
    protected void createFieldEditors() {

        IInformationPresenter presenter = new AbstractTooltipInformationPresenter() {
            @Override
            protected void onUpdatePresentation(String hoverInfo, TextPresentation presentation) {
            }

            @Override
            protected void onHandleClick(Object data) {
            }
        };

        final Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(SAVE_ACTIONS_ONLY_ON_WORKSPACE_FILES,
                "Apply save actions only to files in the workspace?", p));

        addField(new BooleanFieldEditor(FORMAT_BEFORE_SAVING, "Auto-format editor contents before saving?", p));

        addField(new LinkFieldEditor("link_formatpreferences", "Note: config in <a>code formatting preferences</a>", p,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.python.pydev.plugin.pyCodeFormatterPage";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                }));

        // Sort imports when file is saved?
        sortImportsOnSave =
                new BooleanFieldEditor(SORT_IMPORTS_ON_SAVE, "Sort imports on save?", p);
        addField(sortImportsOnSave);

        addField(new LinkFieldEditor("link_importpreferences",
                "Note: config in <a>code style: imports preferences</a>", p,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.python.pydev.ui.importsconf.ImportsPreferencesPage";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                }));

        tooltipPresenter = new ToolTipPresenterHandler(p.getShell(), presenter,
                "Tip: Click link to open SimpleDateFormat Java docs online.");

        // Enable date field action editor (boolean)
        IPreferenceStore prefStore = getPreferenceStore();
        final String fieldName = prefStore.getString(DATE_FIELD_NAME);
        final String enableDateFieldActionEditorTooltip =
                String.format(enableDateFieldActionEditorTooltipFormat, fieldName);

        enableDateFieldActionEditor =
                new BooleanFieldEditor(ENABLE_DATE_FIELD_ACTION, "Update date field?", p);

        enableDateFieldActionEditor.getDescriptionControl(p).setToolTipText(enableDateFieldActionEditorTooltip);
        addField(enableDateFieldActionEditor);

        // Date field name editor (string)

        fieldNameEditor = new PydevDateFieldNameEditor(DATE_FIELD_NAME, "Date field name:",
                PydevDateFieldNameEditor.UNLIMITED, p);
        fieldNameEditor.getTextControl(p).setToolTipText(String.format("Default is %s", DEFAULT_DATE_FIELD_NAME));
        fieldNameEditor.setEmptyStringAllowed(false);
        //fieldNameEditor.setValidateStrategy(PydevDateFieldNameEditor.VALIDATE_ON_FOCUS_LOST);
        fieldNameEditor.setEnabled(prefStore.getBoolean(ENABLE_DATE_FIELD_ACTION), p);
        addField(fieldNameEditor);

        // Date format editor (string)

        dateFormatEditor = new StringFieldEditor(DATE_FIELD_FORMAT, "Date field format:", StringFieldEditor.UNLIMITED,
                p);
        dateFormatEditor.getTextControl(p).setToolTipText("Uses Java's SimpleDateFormat tokens.");
        dateFormatEditor.setEmptyStringAllowed(false);
        //dateFormatEditor.setValidateStrategy(StringFieldEditor.VALIDATE_ON_FOCUS_LOST);
        dateFormatEditor.setEnabled(prefStore.getBoolean(ENABLE_DATE_FIELD_ACTION), p);
        addField(dateFormatEditor);

        // Token help editor (link)

        final String dateFormatHelpLinkTooltip = "" +
                "All tokens from Java's SimpleDateFormat class\n" +
                "are supported. The most common ones are:\n" +
                "\n" +
                "y\t\tYear\n" +
                "M\t\tMonth in year\n" +
                "d\t\tDay in month\n" +
                "E\t\tDay name in week\n" +
                "H\t\tHour in day (0-23)\n" +
                "h\t\tHour in am/pm (1-12)\n" +
                "m\t\tMinute in hour\n" +
                "s\t\tSecond in minute\n" +
                "\n" +
                "Enclose literal characters in single quotes.";

        dateFormatHelpLinkEditor =
                new LinkFieldEditor("link_dateFormat", "<a>Supported tokens</a>", p,
                        new PydevSaveActionsPrefPage.PydevSaveActionsPageLinkListener(),
                        dateFormatHelpLinkTooltip, tooltipPresenter);
        addField(dateFormatHelpLinkEditor);

        addField(new LabelFieldEditor("__dummy__",
                "I.e.: __updated__=\"2010-01-01\" will be synched on save.", p));

        addField(new ScopedPreferencesFieldEditor(p, PydevPlugin.DEFAULT_PYDEV_SCOPE, this));

    }

    public void init(IWorkbench workbench) {
    }

    public static boolean getDateFieldActionEnabled(PyEdit pyEdit) {
        return PyScopedPreferences.getBoolean(ENABLE_DATE_FIELD_ACTION, pyEdit);
    }

    public static boolean getSortImportsOnSave(PyEdit pyEdit) {
        return PyScopedPreferences.getBoolean(SORT_IMPORTS_ON_SAVE, pyEdit);
    }

    public static boolean getFormatBeforeSaving(PyEdit pyEdit) {
        return PyScopedPreferences.getBoolean(FORMAT_BEFORE_SAVING, pyEdit);
    }

    public static String getDateFieldName(PyEdit pyEdit) {
        return PyScopedPreferences.getString(DATE_FIELD_NAME, pyEdit, DEFAULT_DATE_FIELD_NAME);
    }

    public static String getDateFieldFormat(PyEdit pyEdit) {
        return PyScopedPreferences.getString(DATE_FIELD_FORMAT, pyEdit, DEFAULT_DATE_FIELD_FORMAT);
    }

    public static boolean getAutoformatOnlyWorkspaceFiles(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(SAVE_ACTIONS_ONLY_ON_WORKSPACE_FILES, projectAdaptable);
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        updateDateFieldStringEditorState();
    }

    private void updateDateFieldStringEditorState() {
        final boolean val = enableDateFieldActionEditor.getBooleanValue();
        final Composite p = getFieldEditorParent();
        dateFormatEditor.setEnabled(val, p);
        fieldNameEditor.setEnabled(val, p);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (enableDateFieldActionEditor.equals(event.getSource())) {
            updateDateFieldStringEditorState();
        }
        setValid((dateFormatEditor.isValid() && fieldNameEditor.isValid()));
        updatePageButtons();
    }

    private void updatePageButtons() {
        final boolean valid = isValid();
        final Button defaultButton = getShell().getDefaultButton();
        if (!valid) {
            getApplyButton().setEnabled(false);
            if (defaultButton != null) {
                defaultButton.setEnabled(false);
            }
        } else {
            getApplyButton().setEnabled(true);
            if (defaultButton != null) {
                defaultButton.setEnabled(true);
            }
        }
    }

}
