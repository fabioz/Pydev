/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 29, 2006
 */
package org.python.pydev.editor.preferences;

import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.python.pydev.core.preferences.PyDevTypingPreferences;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;
import org.python.pydev.shared_ui.field_editors.BooleanFieldEditorCustom;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;

/**
 * This class is the class that resulted of the separation of the PydevPrefs because
 * it was too big.
 * 
 * @author Fabio
 */
public class PydevTypingPreferencesPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public PydevTypingPreferencesPage() {
        super(GRID);
        setDescription("Editor");
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        final Composite initialParent = getFieldEditorParent();
        final Composite p = initialParent;

        String preference = PyDevTypingPreferences.AUTO_LINK;
        String text = "Enable link on automatic parenthesis or literals closing?";
        String tooltip = "Enabling this option will enable the linking mode after a parenthesis or literal is auto-closed.";

        addBooleanField(p, preference, text, tooltip);

        //auto par
        addBooleanField(
                p,
                PyDevTypingPreferences.AUTO_PAR,
                "Automatic parentheses insertion",
                "Enabling this option will enable automatic insertion of parentheses.  "
                        + "Specifically, whenever you hit a brace such as '(', '{', or '[', its related peer will be inserted "
                        + "and your cursor will be placed between the two braces.");

        //smart indent?
        final BooleanFieldEditorCustom useSmartIndent = addBooleanField(p, PyDevTypingPreferences.SMART_INDENT_PAR, "Use smart-indent?", "");

        //pep-8 indent?
        final BooleanFieldEditorCustom usePep8Indent = addBooleanField(p, PyDevTypingPreferences.INDENT_AFTER_PAR_AS_PEP8,
                "    After {, [, ( indent as pep-8.\n", "");

        final LabelFieldEditor labelPep8_1 = new LabelFieldEditor("__UNUSED__00",
                "            I.e.: add indentation plus additional level right after", p);
        addField(labelPep8_1);
        final LabelFieldEditor labelPep8_2 = new LabelFieldEditor("__UNUSED__01", "", p);
        addField(labelPep8_2); // fill second column
        final LabelFieldEditor labelPep8_3 = new LabelFieldEditor("__UNUSED__02",
                "            braces or indent to braces after another token.", p);
        addField(labelPep8_3);

        // indent
        final BooleanFieldEditorCustom autoIndentToParLevel = addBooleanField(p, PyDevTypingPreferences.AUTO_INDENT_TO_PAR_LEVEL,
                "    After {, [, ( indent to its level (indents by tabs if unchecked)", "");

        final IntegerFieldEditor indentationLevelsToAddField = new IntegerFieldEditor(PyDevTypingPreferences.AUTO_INDENT_AFTER_PAR_WIDTH,
                "        Number of indentation levels to add:", p, 1);
        addField(indentationLevelsToAddField);
        final Runnable fixEnablement = new Runnable() {

            @Override
            public void run() {
                boolean useSmartIndentBool = useSmartIndent.getBooleanValue();
                boolean usePep8IndentBool = usePep8Indent.getBooleanValue();
                boolean useAutoIndentToParLevelBool = autoIndentToParLevel.getBooleanValue();

                fixParensIndentEnablement(p, usePep8Indent, autoIndentToParLevel, indentationLevelsToAddField,
                        labelPep8_1, labelPep8_2, labelPep8_3,
                        useSmartIndentBool, usePep8IndentBool, useAutoIndentToParLevelBool);
            }
        };

        autoIndentToParLevel.getCheckBox(p).addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fixEnablement.run();
            }
        });

        usePep8Indent.getCheckBox(p).addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fixEnablement.run();
            }
        });

        useSmartIndent.getCheckBox(p).addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fixEnablement.run();
            }
        });

        fixParensIndentEnablement(p, usePep8Indent, autoIndentToParLevel, indentationLevelsToAddField, labelPep8_1,
                labelPep8_2, labelPep8_3,
                getPreferenceStore().getBoolean(PyDevTypingPreferences.SMART_INDENT_PAR),
                getPreferenceStore().getBoolean(PyDevTypingPreferences.INDENT_AFTER_PAR_AS_PEP8),
                getPreferenceStore().getBoolean(PyDevTypingPreferences.AUTO_INDENT_TO_PAR_LEVEL));

        //auto dedent 'else:'
        addBooleanField(p, PyDevTypingPreferences.AUTO_DEDENT_ELSE, "Automatic dedent of 'else:' and 'elif:'", "");

        //auto braces
        addBooleanField(
                p,
                PyDevTypingPreferences.AUTO_BRACES,
                "Automatically skip matching braces when typing",
                "Enabling this option will enable automatically skipping matching braces "
                        + "if you try to insert them.  For example, if you have the following code:\n\n"
                        + "def function(self):\n\n"
                        + "...with your cursor before the end parenthesis (after the 'f' in \"self\"), typing a ')' will "
                        + "simply move the cursor to the position after the ')' without inserting a new one.");

        //auto colon
        addBooleanField(
                p,
                PyDevTypingPreferences.AUTO_COLON,
                "Automatic colon detection",
                "Enabling this feature will enable the editor to detect if you are trying "
                        + "to enter a colon which is already there.  Instead of inserting another colon, the editor will "
                        + "simply move your cursor to the next position after the colon.");

        //auto literals
        addBooleanField(p, PyDevTypingPreferences.AUTO_LITERALS, "Automatic literal closing", "Automatically close literals "
                + "(when ' or \" is added, another one is added to close it).");

        //auto import str
        addBooleanField(p, PyDevTypingPreferences.AUTO_WRITE_IMPORT_STR, "Automatic insertion of the 'import' string on 'from xxx' ",
                "Enabling this will allow the editor to automatically write the"
                        + "'import' string when you write a space after you've written 'from xxx '.");

        addBooleanField(p, PyDevTypingPreferences.AUTO_ADD_SELF, "Add 'self' automatically when declaring methods?", "");

        KeySequence down = KeyBindingHelper.getCommandKeyBinding(ITextEditorActionDefinitionIds.MOVE_LINES_DOWN);
        KeySequence up = KeyBindingHelper.getCommandKeyBinding(ITextEditorActionDefinitionIds.MOVE_LINES_UP);
        String downKey = down != null ? down.format() : "Alt+Down"; //set the default if not there
        String upKey = up != null ? up.format() : "Alt+Up"; //set the default if not there
        addBooleanField(p, PyDevTypingPreferences.SMART_LINE_MOVE,
                StringUtils.format("Smart move for line up  (%s) and line down (%s)?.", upKey, downKey), "");

        addField(new LabelFieldEditor("__UNUSED__", "Note: smart move line up/down change applied on editor restart.",
                p));

        addField(new ScopedPreferencesFieldEditor(p, SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE, this));

    }

    private BooleanFieldEditorCustom addBooleanField(Composite p, String preference, String text, String tooltip) {
        BooleanFieldEditorCustom field = new BooleanFieldEditorCustom(preference, text,
                BooleanFieldEditor.DEFAULT, p);
        addField(field);
        field.setTooltip(p, WrapAndCaseUtils.wrap(tooltip, PyDevTypingPreferences.TOOLTIP_WIDTH));
        return field;
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    private void fixParensIndentEnablement(final Composite p, final BooleanFieldEditorCustom usePep8Indent,
            final BooleanFieldEditorCustom autoIndentToParLevel, final IntegerFieldEditor indentationLevelsToAddField,
            LabelFieldEditor labelPep8_1, LabelFieldEditor labelPep8_2, LabelFieldEditor labelPep8_3,
            boolean useSmartIndentBool, boolean usePep8IndentBool, boolean useAutoIndentToParLevelBool) {
        if (!useSmartIndentBool) {
            // Disable all
            usePep8Indent.setEnabled(false, p);
            labelPep8_1.setEnabled(false, p);
            labelPep8_2.setEnabled(false, p);
            labelPep8_3.setEnabled(false, p);
            autoIndentToParLevel.setEnabled(false, p);
            indentationLevelsToAddField.setEnabled(false, p);
        } else {
            usePep8Indent.setEnabled(true, p);
            labelPep8_1.setEnabled(true, p);
            labelPep8_2.setEnabled(true, p);
            labelPep8_3.setEnabled(true, p);
            // Smart indent enabled, let's see if pep-8 is enabled
            if (usePep8IndentBool) {
                autoIndentToParLevel.setEnabled(false, p);
                indentationLevelsToAddField.setEnabled(false, p);

            } else {
                autoIndentToParLevel.setEnabled(true, p);
                indentationLevelsToAddField.setEnabled(!useAutoIndentToParLevelBool, p);
            }
        }
    }
}
