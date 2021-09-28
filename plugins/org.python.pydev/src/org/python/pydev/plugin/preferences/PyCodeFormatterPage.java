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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.formatter.FormatStd.FormatterEnum;
import org.python.pydev.core.formatter.PyFormatterPreferences;
import org.python.pydev.editor.StyledTextForShowingCodeFactory;
import org.python.pydev.plugin.PyDevUiPrefs;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.field_editors.BooleanFieldEditorCustom;
import org.python.pydev.shared_ui.field_editors.ComboFieldEditor;
import org.python.pydev.shared_ui.field_editors.CustomStringFieldEditor;
import org.python.pydev.shared_ui.field_editors.FileFieldEditorCustom;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.field_editors.RadioGroupFieldEditor;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeFormatterPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private StyledText labelExample;
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
    private ComboFieldEditor formatterStyle;
    private Composite fieldParent;
    private CustomStringFieldEditor autopep8Parameters;
    private CustomStringFieldEditor blackParameters;
    private LinkFieldEditor interpreterLink;
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

    private static final String[][] ENTRIES_AND_VALUES_FOR_FORMATTER = new String[][] {
            { "PyDev.Formatter", FormatterEnum.PYDEVF.toString() },
            { "autopep8", FormatterEnum.AUTOPEP8.toString() },
            { "Black", FormatterEnum.BLACK.toString() },
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
    private RadioGroupFieldEditor blackFormatterLocation;
    private FileFieldEditorCustom blackFileField;

    public static final String[][] SEARCH_FORMATTER_LOCATION_OPTIONS = new String[][] {
            { "Search in interpreter", PyFormatterPreferences.LOCATION_SEARCH },
            { "Specify Location", PyFormatterPreferences.LOCATION_SPECIFY },
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

        formatterStyle = new ComboFieldEditor(PyFormatterPreferences.FORMATTER_STYLE, "Formatter style?",
                ENTRIES_AND_VALUES_FOR_FORMATTER, p);
        addField(formatterStyle);

        blackFormatterLocation = new RadioGroupFieldEditor(PyFormatterPreferences.BLACK_FORMATTER_LOCATION_OPTION,
                "Black executable", 2, SEARCH_FORMATTER_LOCATION_OPTIONS, p);

        for (Button b : blackFormatterLocation.getRadioButtons()) {
            b.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateState();
                }
            });
        }

        addField(blackFormatterLocation);
        blackFileField = new FileFieldEditorCustom(PyFormatterPreferences.BLACK_FORMATTER_FILE_LOCATION,
                "Location of the black executable:",
                p, 1);
        addField(blackFileField);

        interpreterLink = new LinkFieldEditor("link_autopep8_interpreter",
                "Note: the default configured <a>Python Interpreter</a> will be used to execute autopep8.py/black", p,
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
        addField(interpreterLink);

        autopep8Parameters = new CustomStringFieldEditor(PyFormatterPreferences.AUTOPEP8_PARAMETERS,
                "Parameters for autopep8 (i.e.: -a for aggressive, --ignore E24)", p);
        addField(autopep8Parameters);

        blackParameters = new CustomStringFieldEditor(PyFormatterPreferences.BLACK_PARAMETERS,
                "Parameters for black (i.e.: -S don't convert single to double quotes)", p);
        addField(blackParameters);

        onlyChangedLines = createBooleanFieldEditorCustom(PyFormatterPreferences.FORMAT_ONLY_CHANGED_LINES,
                "On save, only apply formatting in changed lines?", p);
        addField(onlyChangedLines);

        createTabs(p);

        spaceAfterComma = createBooleanFieldEditorCustom(PyFormatterPreferences.USE_SPACE_AFTER_COMMA,
                "Use space after commas?",
                spacingParent);
        addField(spaceAfterComma);

        spaceForParentesis = createBooleanFieldEditorCustom(PyFormatterPreferences.USE_SPACE_FOR_PARENTESIS,
                "Use space before and after parenthesis?", spacingParent);
        addField(spaceForParentesis);

        assignWithSpaceInsideParentesis = createBooleanFieldEditorCustom(
                PyFormatterPreferences.USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS,
                "Use space before and after assign for keyword arguments?", spacingParent);
        addField(assignWithSpaceInsideParentesis);

        operatorsWithSpace = createBooleanFieldEditorCustom(PyFormatterPreferences.USE_OPERATORS_WITH_SPACE,
                "Use space before and after operators? (+, -, /, *, //, **, etc.)", spacingParent);
        addField(operatorsWithSpace);

        rightTrimLines = createBooleanFieldEditorCustom(PyFormatterPreferences.TRIM_LINES, "Right trim lines?",
                spacingParent);
        addField(rightTrimLines);

        rightTrimMultilineLiterals = createBooleanFieldEditorCustom(PyFormatterPreferences.TRIM_MULTILINE_LITERALS,
                "Right trim multi-line string literals?", spacingParent);
        addField(rightTrimMultilineLiterals);

        spacesBeforeComment = new ComboFieldEditor(PyFormatterPreferences.SPACES_BEFORE_COMMENT,
                "Spaces before a comment?",
                ENTRIES_AND_VALUES_FOR_SPACES, commentsParent);
        addField(spacesBeforeComment);

        spacesInStartComment = new ComboFieldEditor(PyFormatterPreferences.SPACES_IN_START_COMMENT,
                "Spaces in comment start?",
                ENTRIES_AND_VALUES_FOR_SPACES2, commentsParent);
        addField(spacesInStartComment);

        addNewLineAtEndOfFile = createBooleanFieldEditorCustom(PyFormatterPreferences.ADD_NEW_LINE_AT_END_OF_FILE,
                "Add new line at end of file?", blankLinesParent);
        addField(addNewLineAtEndOfFile);

        manageBlankLines = createBooleanFieldEditorCustom(PyFormatterPreferences.MANAGE_BLANK_LINES,
                "Manage blank lines?\n(will convert 2+ subsequent blank lines to 1)", blankLinesParent);
        addField(manageBlankLines);

        blankLinesTopLevel = new IntegerFieldEditor(PyFormatterPreferences.BLANK_LINES_TOP_LEVEL,
                "Blank lines before/after top level class/method?", blankLinesParent);
        addField(blankLinesTopLevel);

        blankLinesInner = new IntegerFieldEditor(PyFormatterPreferences.BLANK_LINES_INNER,
                "Blank lines before/after non top level class/method?", blankLinesParent);
        addField(blankLinesInner);

        formatAndStyleRangeHelper = new StyledTextForShowingCodeFactory();
        labelExample = formatAndStyleRangeHelper.createStyledTextForCodePresentation(p);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        labelExample.setLayoutData(layoutData);

        addField(new ScopedPreferencesFieldEditor(p, SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE, this));

        RunInUiThread.async(new Runnable() {

            @Override
            public void run() {
                if (!disposed) {
                    updateState();
                }
            }
        }, false);
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
        formatterStyle.getCombo().addSelectionListener(listener);
        manageBlankLines.getCheckBox(blankLinesParent).addSelectionListener(listener);

        updateState();

        // And update the example when it's already there
        updateLabelExampleNow(this.getFormatFromEditorContents());
    }

    private FormatterEnum getComboFormatterStyle() {
        String comboValue = formatterStyle.getComboValue();
        return FormatStd.getFormatterEnumFromStr(comboValue);
    }

    private void updateState() {
        FormatterEnum currFormatterStyle = getComboFormatterStyle();
        autopep8Parameters.setVisible(currFormatterStyle == FormatterEnum.AUTOPEP8, fieldParent);
        blackParameters.setVisible(currFormatterStyle == FormatterEnum.BLACK, fieldParent);

        switch (currFormatterStyle) {
            case AUTOPEP8:
            case BLACK:
                blackFormatterLocation.setVisible(currFormatterStyle == FormatterEnum.BLACK, fieldParent);
                boolean showFile = currFormatterStyle == FormatterEnum.BLACK && PyFormatterPreferences.LOCATION_SPECIFY
                        .equals(blackFormatterLocation.getRadioValue());
                blackFileField.setVisible(showFile);
                interpreterLink.setVisible(!showFile);

                assignWithSpaceInsideParentesis.setVisible(false, spacingParent);
                operatorsWithSpace.setVisible(false, spacingParent);
                spaceForParentesis.setVisible(false, spacingParent);
                spaceAfterComma.setVisible(false, spacingParent);
                rightTrimLines.setVisible(false, spacingParent);
                rightTrimMultilineLiterals.setVisible(false, spacingParent);

                addNewLineAtEndOfFile.setVisible(false, blankLinesParent);
                manageBlankLines.setVisible(false, blankLinesParent);
                blankLinesTopLevel.setEnabled(false, blankLinesParent);
                blankLinesInner.setEnabled(false, blankLinesParent);

                spacesBeforeComment.setEnabled(false, commentsParent);
                spacesInStartComment.setEnabled(false, commentsParent);

                onlyChangedLines.setVisible(false, fieldParent);
                setVisible(tabFolder, false);
                break;

            default:
                blackFormatterLocation.setVisible(false, fieldParent);
                blackFileField.setVisible(false);
                interpreterLink.setVisible(false);

                assignWithSpaceInsideParentesis.setVisible(true, spacingParent);
                operatorsWithSpace.setVisible(true, spacingParent);
                spaceForParentesis.setVisible(true, spacingParent);
                spaceAfterComma.setVisible(true, spacingParent);
                rightTrimLines.setVisible(true, spacingParent);
                rightTrimMultilineLiterals.setVisible(true, spacingParent);

                addNewLineAtEndOfFile.setVisible(true, blankLinesParent);
                manageBlankLines.setVisible(true, blankLinesParent);
                if (manageBlankLines.getBooleanValue()) {
                    blankLinesTopLevel.setEnabled(true, blankLinesParent);
                    blankLinesInner.setEnabled(true, blankLinesParent);

                } else {
                    blankLinesTopLevel.setEnabled(false, blankLinesParent);
                    blankLinesInner.setEnabled(false, blankLinesParent);
                }

                spacesBeforeComment.setEnabled(true, commentsParent);
                spacesInStartComment.setEnabled(true, commentsParent);

                onlyChangedLines.setVisible(true, fieldParent);
                setVisible(tabFolder, true);
        }
        fieldParent.layout(true);
    }

    private void setVisible(Control control, boolean visible) {
        control.setVisible(visible);
        GridData layoutData = (GridData) control.getLayoutData();
        layoutData.exclude = !visible;
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
        formatStd.formatterStyle = getComboFormatterStyle();
        formatStd.autopep8Parameters = this.autopep8Parameters.getStringValue();
        formatStd.blackParameters = this.blackParameters.getStringValue();
        formatStd.searchBlackInInterpreter = !PyFormatterPreferences.LOCATION_SPECIFY
                .equals(this.blackFormatterLocation.getRadioValue());
        formatStd.blackExecutableLocation = this.blackFileField.getStringValue();
        formatStd.updateFormatterStyle();
        return formatStd;
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    public void dispose() {
        disposed = true;
        super.dispose();
        if (formatAndStyleRangeHelper != null) {
            formatAndStyleRangeHelper.dispose();
        }
    }

}
