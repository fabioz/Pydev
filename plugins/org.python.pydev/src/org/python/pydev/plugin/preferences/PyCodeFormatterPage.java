/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.editor.StyledTextForShowingCodeFactory;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.editor.preferences.PyScopedPreferences;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.field_editors.BooleanFieldEditorCustom;
import org.python.pydev.shared_ui.field_editors.ComboFieldEditor;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeFormatterPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String FORMAT_WITH_AUTOPEP8 = "FORMAT_WITH_AUTOPEP8";
    public static final boolean DEFAULT_FORMAT_WITH_AUTOPEP8 = false;

    public static final String AUTOPEP8_PARAMETERS = "AUTOPEP8_PARAMETERS";

    public static final String FORMAT_ONLY_CHANGED_LINES = "FORMAT_ONLY_CHANGED_LINES";
    public static final boolean DEFAULT_FORMAT_ONLY_CHANGED_LINES = false;

    public static final String TRIM_LINES = "TRIM_EMPTY_LINES";
    public static final boolean DEFAULT_TRIM_LINES = false;

    public static final String TRIM_MULTILINE_LITERALS = "TRIM_MULTILINE_LITERALS";
    public static final boolean DEFAULT_TRIM_MULTILINE_LITERALS = false;

    public static final String ADD_NEW_LINE_AT_END_OF_FILE = "ADD_NEW_LINE_AT_END_OF_FILE";
    public static final boolean DEFAULT_ADD_NEW_LINE_AT_END_OF_FILE = true;

    //a, b, c
    public static final String USE_SPACE_AFTER_COMMA = "USE_SPACE_AFTER_COMMA";
    public static final boolean DEFAULT_USE_SPACE_AFTER_COMMA = true;

    //call( a )
    public static final String USE_SPACE_FOR_PARENTESIS = "USE_SPACE_FOR_PARENTESIS";
    public static final boolean DEFAULT_USE_SPACE_FOR_PARENTESIS = false;

    //call(a = 1)
    public static final String USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS = "USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS";
    public static final boolean DEFAULT_USE_ASSIGN_WITH_PACES_INSIDE_PARENTESIS = false;

    //operators =, !=, <, >, //, etc.
    public static final String USE_OPERATORS_WITH_SPACE = "USE_OPERATORS_WITH_SPACE";
    public static final boolean DEFAULT_USE_OPERATORS_WITH_SPACE = true;

    //Spaces before '#'.
    public static final String SPACES_BEFORE_COMMENT = "SPACES_BEFORE_COMMENT";
    public static final int DEFAULT_SPACES_BEFORE_COMMENT = 2; //pep-8 says 2 spaces before inline comment.

    //Spaces after '#'.
    public static final String SPACES_IN_START_COMMENT = "SPACES_IN_START_COMMENT";
    public static final int DEFAULT_SPACES_IN_START_COMMENT = 1; //pep-8 says 1 space after '#'

    private StyledText labelExample;
    private BooleanFieldEditorCustom formatWithAutoPep8;
    private BooleanFieldEditorCustom spaceAfterComma;
    private BooleanFieldEditorCustom onlyChangedLines;
    private BooleanFieldEditorCustom spaceForParentesis;
    private BooleanFieldEditorCustom assignWithSpaceInsideParentesis;
    private BooleanFieldEditorCustom operatorsWithSpace;
    private BooleanFieldEditorCustom rightTrimLines;
    private BooleanFieldEditorCustom rightTrimMultilineLiterals;
    private BooleanFieldEditorCustom addNewLineAtEndOfFile;
    private StyledTextForShowingCodeFactory formatAndStyleRangeHelper;
    private ComboFieldEditor spacesBeforeComment;
    private ComboFieldEditor spacesInStartComment;
    private Composite fieldParent;
    private StringFieldEditor autopep8Parameters;
    private LinkFieldEditor autopep8Link;
    private boolean disposed = false;

    public PyCodeFormatterPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    private static final String[][] ENTRIES_AND_VALUES_FOR_SPACES = new String[][] {
            { "Don't change manual formatting", Integer.toString(FormatStd.DONT_HANDLE_SPACES) },
            { "No spaces", "0" },
            { "1 space", "1" },
            { "2 spaces", "2" },
            { "3 spaces", "3" },
            { "4 spaces", "4" },
    };

    private static final String[][] ENTRIES_AND_VALUES_FOR_SPACES2 = new String[][] {
            { "Don't change manual formatting", Integer.toString(FormatStd.DONT_HANDLE_SPACES) }, //0 and -1 means the same thing here.
            { "At least 1 space", "1" },
            { "At least 2 spaces", "2" },
            { "At least 3 spaces", "3" },
            { "At least 4 spaces", "4" },
    };

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();
        this.fieldParent = p;

        addField(new LinkFieldEditor("link_saveactions", "Note: view <a>save actions</a> to auto-format on save.", p,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.python.pydev.editor.saveactions.PydevSaveActionsPrefPage";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                }));

        formatWithAutoPep8 = createBooleanFieldEditorCustom(FORMAT_WITH_AUTOPEP8,
                "Use autopep8.py for code formatting?", p);
        addField(formatWithAutoPep8);

        autopep8Link = new LinkFieldEditor("link_autopep8_interpreter",
                "Note: the default configured <a>Python Interpreter</a> will be used to execute autopep8.py", p,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPagePython";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        addField(autopep8Link);

        autopep8Parameters = new StringFieldEditor(AUTOPEP8_PARAMETERS,
                "Parameters for autopep8 (i.e.: -a for aggressive, --ignore E24)", p);
        addField(autopep8Parameters);

        onlyChangedLines = createBooleanFieldEditorCustom(FORMAT_ONLY_CHANGED_LINES,
                "On save, only apply formatting in changed lines?", p);
        addField(onlyChangedLines);

        spaceAfterComma = createBooleanFieldEditorCustom(USE_SPACE_AFTER_COMMA, "Use space after commas?", p);
        addField(spaceAfterComma);

        spaceForParentesis = createBooleanFieldEditorCustom(USE_SPACE_FOR_PARENTESIS,
                "Use space before and after parenthesis?", p);
        addField(spaceForParentesis);

        assignWithSpaceInsideParentesis = createBooleanFieldEditorCustom(USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS,
                "Use space before and after assign for keyword arguments?", p);
        addField(assignWithSpaceInsideParentesis);

        operatorsWithSpace = createBooleanFieldEditorCustom(USE_OPERATORS_WITH_SPACE,
                "Use space before and after operators? (+, -, /, *, //, **, etc.)", p);
        addField(operatorsWithSpace);

        rightTrimLines = createBooleanFieldEditorCustom(TRIM_LINES, "Right trim lines?", p);
        addField(rightTrimLines);

        rightTrimMultilineLiterals = createBooleanFieldEditorCustom(TRIM_MULTILINE_LITERALS,
                "Right trim multi-line string literals?", p);
        addField(rightTrimMultilineLiterals);

        addNewLineAtEndOfFile = createBooleanFieldEditorCustom(ADD_NEW_LINE_AT_END_OF_FILE,
                "Add new line at end of file?", p);
        addField(addNewLineAtEndOfFile);

        spacesBeforeComment = new ComboFieldEditor(SPACES_BEFORE_COMMENT, "Spaces before a comment?",
                ENTRIES_AND_VALUES_FOR_SPACES, p);
        addField(spacesBeforeComment);

        spacesInStartComment = new ComboFieldEditor(SPACES_IN_START_COMMENT, "Spaces in comment start?",
                ENTRIES_AND_VALUES_FOR_SPACES2, p);
        addField(spacesInStartComment);

        formatAndStyleRangeHelper = new StyledTextForShowingCodeFactory();
        labelExample = formatAndStyleRangeHelper.createStyledTextForCodePresentation(p);
        GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
        labelExample.setLayoutData(layoutData);

        addField(new ScopedPreferencesFieldEditor(p, PydevPlugin.DEFAULT_PYDEV_SCOPE, this));
    }

    @Override
    protected void initialize() {
        super.initialize();

        //After initializing, let's check the proper state based on pep8.
        Button checkBox = formatWithAutoPep8.getCheckBox(fieldParent);
        checkBox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateState();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
        updateState();

        // And update the example when it's already there
        updateLabelExampleNow(this.getFormatFromEditorContents());
    }

    private void updateState() {
        if (formatWithAutoPep8.getBooleanValue()) {
            assignWithSpaceInsideParentesis.setEnabled(false, fieldParent);
            operatorsWithSpace.setEnabled(false, fieldParent);
            spaceForParentesis.setEnabled(false, fieldParent);
            spaceAfterComma.setEnabled(false, fieldParent);
            addNewLineAtEndOfFile.setEnabled(false, fieldParent);
            rightTrimLines.setEnabled(false, fieldParent);
            rightTrimMultilineLiterals.setEnabled(false, fieldParent);
            spacesBeforeComment.setEnabled(false, fieldParent);
            spacesInStartComment.setEnabled(false, fieldParent);
            onlyChangedLines.setEnabled(false, fieldParent);
            autopep8Parameters.setEnabled(true, fieldParent);
            autopep8Link.setEnabled(true, fieldParent);
        } else {
            assignWithSpaceInsideParentesis.setEnabled(true, fieldParent);
            operatorsWithSpace.setEnabled(true, fieldParent);
            spaceForParentesis.setEnabled(true, fieldParent);
            spaceAfterComma.setEnabled(true, fieldParent);
            addNewLineAtEndOfFile.setEnabled(true, fieldParent);
            rightTrimLines.setEnabled(true, fieldParent);
            rightTrimMultilineLiterals.setEnabled(true, fieldParent);
            spacesBeforeComment.setEnabled(true, fieldParent);
            spacesInStartComment.setEnabled(true, fieldParent);
            onlyChangedLines.setEnabled(true, fieldParent);
            autopep8Parameters.setEnabled(false, fieldParent);
            autopep8Link.setEnabled(false, fieldParent);
        }

    }

    private BooleanFieldEditorCustom createBooleanFieldEditorCustom(String name, String label, Composite parent) {
        return new BooleanFieldEditorCustom(name, label, BooleanFieldEditor.SEPARATE_LABEL, parent);
    }

    // Note: no locking is needed since we're doing everything in the UI thread.
    private Runnable currentRunnable;

    private void updateLabelExample(final FormatStd formatStd) {
        if (!disposed) {
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    if (disposed) {
                        currentRunnable = null;
                        return;
                    }

                    if (currentRunnable == this) {
                        updateLabelExampleNow(formatStd);
                        currentRunnable = null;
                    }
                }
            };
            currentRunnable = runnable;
            // Give a timeout before updating (otherwise when changing the text for the autopep8 integration
            // it becomes slow).
            Display.getCurrent().timerExec(400, runnable);
        }
    }

    private void updateLabelExampleNow(FormatStd formatStd) {

        String str = "class Example(object):             \n" +
                "                                   \n" +
                "    def Call(self, param1=None):   \n" +
                "        '''docstring'''            \n" +
                "        return param1 + 10 * 10    \n" +
                "                                   \n" +
                "    def Call2(self): #Comment      \n" +
                "        #Comment                   \n" +
                "        return self.Call(param1=10)" +
                "";
        Tuple<String, StyleRange[]> result = formatAndStyleRangeHelper.formatAndGetStyleRanges(formatStd, str,
                PydevPrefs.getChainedPrefStore(), true);
        labelExample.setText(result.o1);
        labelExample.setStyleRanges(result.o2);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        updateLabelExample(getFormatFromEditorContents());
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        updateLabelExample(getFormatFromEditorContents());
        updateState();
    }

    private FormatStd getFormatFromEditorContents() {
        FormatStd formatStd = new FormatStd();
        formatStd.assignWithSpaceInsideParens = this.assignWithSpaceInsideParentesis.getBooleanValue();
        formatStd.operatorsWithSpace = operatorsWithSpace.getBooleanValue();
        formatStd.parametersWithSpace = spaceForParentesis.getBooleanValue();
        formatStd.spaceAfterComma = spaceAfterComma.getBooleanValue();
        formatStd.addNewLineAtEndOfFile = addNewLineAtEndOfFile.getBooleanValue();
        formatStd.trimLines = rightTrimLines.getBooleanValue();
        formatStd.trimMultilineLiterals = rightTrimMultilineLiterals.getBooleanValue();
        formatStd.spacesBeforeComment = Integer.parseInt(spacesBeforeComment.getComboValue());
        formatStd.spacesInStartComment = Integer.parseInt(spacesInStartComment.getComboValue());
        formatStd.formatWithAutopep8 = this.formatWithAutoPep8.getBooleanValue();
        formatStd.autopep8Parameters = this.autopep8Parameters.getStringValue();
        formatStd.updateAutopep8();
        return formatStd;
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    public static boolean getFormatWithAutopep8(IAdaptable projectAdaptable) {
        return getBoolean(FORMAT_WITH_AUTOPEP8, projectAdaptable);
    }

    public static boolean getBoolean(String setting, IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(setting, projectAdaptable);
    }

    protected static String getString(String setting, IAdaptable projectAdaptable) {
        return PyScopedPreferences.getString(setting, projectAdaptable);
    }

    public static String getAutopep8Parameters(IAdaptable projectAdaptable) {
        return getString(AUTOPEP8_PARAMETERS, projectAdaptable);
    }

    public static boolean getFormatOnlyChangedLines(IAdaptable projectAdaptable) {
        if (getFormatWithAutopep8(projectAdaptable)) {
            return false; //i.e.: not available with autopep8.
        }
        return getBoolean(FORMAT_ONLY_CHANGED_LINES, projectAdaptable);
    }

    public static boolean getAddNewLineAtEndOfFile(IAdaptable projectAdaptable) {
        return getBoolean(ADD_NEW_LINE_AT_END_OF_FILE, projectAdaptable);
    }

    public static boolean getTrimLines(IAdaptable projectAdaptable) {
        return getBoolean(TRIM_LINES, projectAdaptable);
    }

    public static boolean getTrimMultilineLiterals(IAdaptable projectAdaptable) {
        return getBoolean(TRIM_MULTILINE_LITERALS, projectAdaptable);
    }

    public static boolean useSpaceAfterComma(IAdaptable projectAdaptable) {
        return getBoolean(USE_SPACE_AFTER_COMMA, projectAdaptable);
    }

    public static boolean useSpaceForParentesis(IAdaptable projectAdaptable) {
        return getBoolean(USE_SPACE_FOR_PARENTESIS, projectAdaptable);
    }

    public static boolean useAssignWithSpacesInsideParenthesis(IAdaptable projectAdaptable) {
        return getBoolean(USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS, projectAdaptable);
    }

    public static boolean useOperatorsWithSpace(IAdaptable projectAdaptable) {
        return getBoolean(USE_OPERATORS_WITH_SPACE, projectAdaptable);
    }

    public static int getSpacesBeforeComment(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SPACES_BEFORE_COMMENT, projectAdaptable, FormatStd.DONT_HANDLE_SPACES);
    }

    public static int getSpacesInStartComment(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SPACES_IN_START_COMMENT, projectAdaptable, FormatStd.DONT_HANDLE_SPACES);
    }

    @Override
    public void dispose() {
        disposed = true;
        super.dispose();
        formatAndStyleRangeHelper.dispose();
    }

}
