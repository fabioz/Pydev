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
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.core.FormatStd;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.editor.StyledTextForShowingCodeFactory;
import org.python.pydev.plugin.PyDevUiPrefs;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
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

    // Leave at most 1 blank line by default
    public static final String MANAGE_BLANK_LINES = "MANAGE_BLANK_LINES";
    public static final boolean DEFAULT_MANAGE_BLANK_LINES = true;

    public static final String BLANK_LINES_TOP_LEVEL = "BLANK_LINES_TOP_LEVEL";
    public static final int DEFAULT_BLANK_LINES_TOP_LEVEL = 2;

    public static final String BLANK_LINES_INNER = "BLANK_LINES_INNER";
    public static final int DEFAULT_BLANK_LINES_INNER = 1;

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
    private TabFolder tabFolder;

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
    private TabItem tabItemSpacing;
    private TabItem tabItemBlankLines;
    private Composite blankLinesParent;
    private Composite spacingParent;
    private TabItem tabItemComments;
    private Composite commentsParent;
    private BooleanFieldEditorCustom manageBlankLines;
    private IntegerFieldEditor blankLinesTopLevel;
    private IntegerFieldEditor blankLinesInner;

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
                        String id = "org.python.pydev.ast.interpreter_managers.interpreterPreferencesPagePython";
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

        createTabs(p);

        spaceAfterComma = createBooleanFieldEditorCustom(USE_SPACE_AFTER_COMMA, "Use space after commas?",
                spacingParent);
        addField(spaceAfterComma);

        spaceForParentesis = createBooleanFieldEditorCustom(USE_SPACE_FOR_PARENTESIS,
                "Use space before and after parenthesis?", spacingParent);
        addField(spaceForParentesis);

        assignWithSpaceInsideParentesis = createBooleanFieldEditorCustom(USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS,
                "Use space before and after assign for keyword arguments?", spacingParent);
        addField(assignWithSpaceInsideParentesis);

        operatorsWithSpace = createBooleanFieldEditorCustom(USE_OPERATORS_WITH_SPACE,
                "Use space before and after operators? (+, -, /, *, //, **, etc.)", spacingParent);
        addField(operatorsWithSpace);

        rightTrimLines = createBooleanFieldEditorCustom(TRIM_LINES, "Right trim lines?", spacingParent);
        addField(rightTrimLines);

        rightTrimMultilineLiterals = createBooleanFieldEditorCustom(TRIM_MULTILINE_LITERALS,
                "Right trim multi-line string literals?", spacingParent);
        addField(rightTrimMultilineLiterals);

        spacesBeforeComment = new ComboFieldEditor(SPACES_BEFORE_COMMENT, "Spaces before a comment?",
                ENTRIES_AND_VALUES_FOR_SPACES, commentsParent);
        addField(spacesBeforeComment);

        spacesInStartComment = new ComboFieldEditor(SPACES_IN_START_COMMENT, "Spaces in comment start?",
                ENTRIES_AND_VALUES_FOR_SPACES2, commentsParent);
        addField(spacesInStartComment);

        addNewLineAtEndOfFile = createBooleanFieldEditorCustom(ADD_NEW_LINE_AT_END_OF_FILE,
                "Add new line at end of file?", blankLinesParent);
        addField(addNewLineAtEndOfFile);

        manageBlankLines = createBooleanFieldEditorCustom(MANAGE_BLANK_LINES,
                "Manage blank lines?\n(will convert 2+ subsequent blank lines to 1)", blankLinesParent);
        addField(manageBlankLines);

        blankLinesTopLevel = new IntegerFieldEditor(BLANK_LINES_TOP_LEVEL,
                "Blank lines before/after top level class/method?", blankLinesParent);
        addField(blankLinesTopLevel);

        blankLinesInner = new IntegerFieldEditor(BLANK_LINES_INNER,
                "Blank lines before/after non top level class/method?", blankLinesParent);
        addField(blankLinesInner);

        formatAndStyleRangeHelper = new StyledTextForShowingCodeFactory();
        labelExample = formatAndStyleRangeHelper.createStyledTextForCodePresentation(p);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        labelExample.setLayoutData(layoutData);

        addField(new ScopedPreferencesFieldEditor(p, SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE, this));
    }

    private void createTabs(Composite p) {
        tabFolder = new TabFolder(p, SWT.None);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.BEGINNING;
        gd.grabExcessVerticalSpace = false;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 2;
        tabFolder.setLayoutData(gd);

        tabItemSpacing = new TabItem(tabFolder, SWT.NONE);
        tabItemSpacing.setText("Spacing");
        spacingParent = new Composite(tabFolder, SWT.NONE);
        tabItemSpacing.setControl(spacingParent);

        tabItemBlankLines = new TabItem(tabFolder, SWT.NONE);
        tabItemBlankLines.setText("Blank lines");
        blankLinesParent = new Composite(tabFolder, SWT.NONE);
        tabItemBlankLines.setControl(blankLinesParent);

        tabItemComments = new TabItem(tabFolder, SWT.NONE);
        tabItemComments.setText("Comments");
        commentsParent = new Composite(tabFolder, SWT.NONE);
        tabItemComments.setControl(commentsParent);
    }

    @Override
    protected void initialize() {
        super.initialize();

        SelectionListener listener = new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateState();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        };

        //After initializing, let's check the proper state based on pep8.
        formatWithAutoPep8.getCheckBox(fieldParent).addSelectionListener(listener);
        manageBlankLines.getCheckBox(blankLinesParent).addSelectionListener(listener);

        updateState();

        // And update the example when it's already there
        updateLabelExampleNow(this.getFormatFromEditorContents());
    }

    private void updateState() {
        if (formatWithAutoPep8.getBooleanValue()) {
            assignWithSpaceInsideParentesis.setEnabled(false, spacingParent);
            operatorsWithSpace.setEnabled(false, spacingParent);
            spaceForParentesis.setEnabled(false, spacingParent);
            spaceAfterComma.setEnabled(false, spacingParent);
            rightTrimLines.setEnabled(false, spacingParent);
            rightTrimMultilineLiterals.setEnabled(false, spacingParent);

            addNewLineAtEndOfFile.setEnabled(false, blankLinesParent);
            manageBlankLines.setEnabled(false, blankLinesParent);
            blankLinesTopLevel.setEnabled(false, blankLinesParent);
            blankLinesInner.setEnabled(false, blankLinesParent);

            spacesBeforeComment.setEnabled(false, commentsParent);
            spacesInStartComment.setEnabled(false, commentsParent);

            onlyChangedLines.setEnabled(false, fieldParent);
            autopep8Parameters.setEnabled(true, fieldParent);
            autopep8Link.setEnabled(true, fieldParent);
        } else {
            assignWithSpaceInsideParentesis.setEnabled(true, spacingParent);
            operatorsWithSpace.setEnabled(true, spacingParent);
            spaceForParentesis.setEnabled(true, spacingParent);
            spaceAfterComma.setEnabled(true, spacingParent);
            rightTrimLines.setEnabled(true, spacingParent);
            rightTrimMultilineLiterals.setEnabled(true, spacingParent);

            addNewLineAtEndOfFile.setEnabled(true, blankLinesParent);
            manageBlankLines.setEnabled(true, blankLinesParent);
            if (manageBlankLines.getBooleanValue()) {
                blankLinesTopLevel.setEnabled(true, blankLinesParent);
                blankLinesInner.setEnabled(true, blankLinesParent);

            } else {
                blankLinesTopLevel.setEnabled(false, blankLinesParent);
                blankLinesInner.setEnabled(false, blankLinesParent);
            }

            spacesBeforeComment.setEnabled(true, commentsParent);
            spacesInStartComment.setEnabled(true, commentsParent);

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

        String str = "" +
                "                                   \n" +
                "                                   \n" +
                "                                   \n" +
                "                                   \n" +
                "class Example(object):             \n" +
                "                                   \n" +
                "    def Call(self, param1=None):   \n" +
                "        '''docstring'''            \n" +
                "        return param1 + 10 * 10    \n" +
                "                                   \n" +
                "                                   \n" +
                "                                   \n" +
                "    def Call2(self): #Comment      \n" +
                "        #Comment                   \n" +
                "        return self.Call(param1=10)" +
                "";
        Tuple<String, StyleRange[]> result = formatAndStyleRangeHelper.formatAndGetStyleRanges(formatStd, str,
                PyDevUiPrefs.getChainedPrefStore(), true);
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
        formatStd.manageBlankLines = manageBlankLines.getBooleanValue();
        try {
            formatStd.blankLinesTopLevel = blankLinesTopLevel.getIntValue();
        } catch (NumberFormatException e1) {
            formatStd.blankLinesTopLevel = 2;
        }
        try {
            formatStd.blankLinesInner = blankLinesInner.getIntValue();
        } catch (NumberFormatException e) {
            formatStd.blankLinesInner = 1;
        }
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
    @Override
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

    public static boolean getManageBlankLines(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(MANAGE_BLANK_LINES, projectAdaptable);
    }

    public static int getBlankLinesTopLevel(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(BLANK_LINES_TOP_LEVEL, projectAdaptable, 0);
    }

    public static int getBlankLinesInner(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(BLANK_LINES_INNER, projectAdaptable, 0);
    }

    @Override
    public void dispose() {
        disposed = true;
        super.dispose();
        formatAndStyleRangeHelper.dispose();
    }

}
