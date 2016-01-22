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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.python.pydev.plugin.PydevPlugin;
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
public class PydevTypingPrefs extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String AUTO_PAR = "AUTO_PAR";
    public static final boolean DEFAULT_AUTO_PAR = true;

    public static final String AUTO_LINK = "AUTO_LINK";
    public static final boolean DEFAULT_AUTO_LINK = false;

    public static final String AUTO_INDENT_TO_PAR_LEVEL = "AUTO_INDENT_TO_PAR_LEVEL";
    public static final boolean DEFAULT_AUTO_INDENT_TO_PAR_LEVEL = true;

    public static final String AUTO_INDENT_AFTER_PAR_WIDTH = "AUTO_INDENT_AFTER_PAR_WIDTH";
    public static final int DEFAULT_AUTO_INDENT_AFTER_PAR_WIDTH = 1;

    public static final String AUTO_DEDENT_ELSE = "AUTO_DEDENT_ELSE";
    public static final boolean DEFAULT_AUTO_DEDENT_ELSE = true;

    public static final String SMART_INDENT_PAR = "SMART_INDENT_PAR";
    public static final boolean DEFAULT_SMART_INDENT_PAR = true;

    public static final String SMART_LINE_MOVE = "SMART_LINE_MOVE";
    //Disabled by default (doesn't seem as useful as I though because Python does not have the end
    //braces and Java does (so, there are a number of cases where the indentation has to be hand-fixed
    //anyways)
    public static final boolean DEFAULT_SMART_LINE_MOVE = false;

    /**
     * fields for automatically replacing a colon
     * @see  
     */
    public static final String AUTO_COLON = "AUTO_COLON";
    public static final boolean DEFAULT_AUTO_COLON = true;

    /**
     * fields for automatically skipping braces
     * @see  org.python.pydev.editor.autoedit.PyAutoIndentStrategy
     */
    public static final String AUTO_BRACES = "AUTO_BRACES";
    public static final boolean DEFAULT_AUTO_BRACES = true;

    /**
     * Used if the 'import' should be written automatically in an from xxx import yyy
     */
    public static final String AUTO_WRITE_IMPORT_STR = "AUTO_WRITE_IMPORT_STR";
    public static final boolean DEFAULT_AUTO_WRITE_IMPORT_STR = true;

    public static final String AUTO_LITERALS = "AUTO_LITERALS";
    public static final boolean DEFAULT_AUTO_LITERALS = true;

    public static final String AUTO_ADD_SELF = "AUTO_ADD_SELF";
    public static final boolean DEFAULT_AUTO_ADD_SELF = true;

    public static final int TOOLTIP_WIDTH = 80;

    public PydevTypingPrefs() {
        super(GRID);
        setDescription("Editor");
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        final Composite initialParent = getFieldEditorParent();
        final Composite p = initialParent;

        String preference = AUTO_LINK;
        String text = "Enable link on automatic parenthesis or literals closing?";
        String tooltip = "Enabling this option will enable the linking mode after a parenthesis or literal is auto-closed.";

        addBooleanField(p, preference, text, tooltip);

        //auto par
        addBooleanField(
                p,
                AUTO_PAR,
                "Automatic parentheses insertion",
                "Enabling this option will enable automatic insertion of parentheses.  "
                        + "Specifically, whenever you hit a brace such as '(', '{', or '[', its related peer will be inserted "
                        + "and your cursor will be placed between the two braces.");

        // indent
        final BooleanFieldEditorCustom autoIndentToParLevel = addBooleanField(p, AUTO_INDENT_TO_PAR_LEVEL,
                "After '(' indent to its level (indents by tabs if unchecked)", "");

        final IntegerFieldEditor intField = new IntegerFieldEditor(AUTO_INDENT_AFTER_PAR_WIDTH,
                "    Number of indentation levels to add:", p, 1);
        addField(intField);
        final Button checkBox = autoIndentToParLevel.getCheckBox(p);
        checkBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                intField.setEnabled(!autoIndentToParLevel.getBooleanValue(), p);
            }
        });
        intField.setEnabled(!getPreferenceStore().getBoolean(AUTO_INDENT_TO_PAR_LEVEL), p);

        //auto dedent 'else:'
        addBooleanField(p, AUTO_DEDENT_ELSE, "Automatic dedent of 'else:' and 'elif:'", "");

        //auto braces
        addBooleanField(
                p,
                AUTO_BRACES,
                "Automatically skip matching braces when typing",
                "Enabling this option will enable automatically skipping matching braces "
                        + "if you try to insert them.  For example, if you have the following code:\n\n"
                        + "def function(self):\n\n"
                        + "...with your cursor before the end parenthesis (after the 'f' in \"self\"), typing a ')' will "
                        + "simply move the cursor to the position after the ')' without inserting a new one.");

        //smart indent
        addBooleanField(p, SMART_INDENT_PAR, "Use smart-indent?", "");

        //auto colon
        addBooleanField(
                p,
                AUTO_COLON,
                "Automatic colon detection",
                "Enabling this feature will enable the editor to detect if you are trying "
                        + "to enter a colon which is already there.  Instead of inserting another colon, the editor will "
                        + "simply move your cursor to the next position after the colon.");

        //auto literals
        addBooleanField(p, AUTO_LITERALS, "Automatic literal closing", "Automatically close literals "
                + "(when ' or \" is added, another one is added to close it).");

        //auto import str
        addBooleanField(p, AUTO_WRITE_IMPORT_STR, "Automatic insertion of the 'import' string on 'from xxx' ",
                "Enabling this will allow the editor to automatically write the"
                        + "'import' string when you write a space after you've written 'from xxx '.");

        addBooleanField(p, AUTO_ADD_SELF, "Add 'self' automatically when declaring methods?", "");

        KeySequence down = KeyBindingHelper.getCommandKeyBinding(ITextEditorActionDefinitionIds.MOVE_LINES_DOWN);
        KeySequence up = KeyBindingHelper.getCommandKeyBinding(ITextEditorActionDefinitionIds.MOVE_LINES_UP);
        String downKey = down != null ? down.format() : "Alt+Down"; //set the default if not there
        String upKey = up != null ? up.format() : "Alt+Up"; //set the default if not there
        addBooleanField(p, SMART_LINE_MOVE,
                StringUtils.format("Smart move for line up  (%s) and line down (%s)?.", upKey, downKey), "");

        addField(new LabelFieldEditor("__UNUSED__", "Note: smart move line up/down change applied on editor restart.",
                p));

        addField(new ScopedPreferencesFieldEditor(p, PydevPlugin.DEFAULT_PYDEV_SCOPE, this));

    }

    private BooleanFieldEditorCustom addBooleanField(Composite p, String preference, String text, String tooltip) {
        BooleanFieldEditorCustom field = new BooleanFieldEditorCustom(preference, text,
                BooleanFieldEditor.DEFAULT, p);
        addField(field);
        field.setTooltip(p, WrapAndCaseUtils.wrap(tooltip, TOOLTIP_WIDTH));
        return field;
    }

    @Override
    public void init(IWorkbench workbench) {

    }
}
