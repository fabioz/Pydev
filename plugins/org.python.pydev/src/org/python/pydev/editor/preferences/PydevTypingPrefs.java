/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 29, 2006
 */
package org.python.pydev.editor.preferences;

import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.python.pydev.bindingutils.KeyBindingHelper;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.docutils.WrapAndCaseUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.AbstractPydevPrefs;

/**
 * This class is the class that resulted of the separation of the PydevPrefs because
 * it was too big.
 * 
 * @author Fabio
 */
public class PydevTypingPrefs extends AbstractPydevPrefs {

    public PydevTypingPrefs() {
        setDescription("Editor");
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        this.fOverlayStore = createOverlayStore();
    }

    protected Control createAppearancePage(Composite parent) {
        Composite appearanceComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        appearanceComposite.setLayout(layout);

        // simply a holder for the current reference for a Button, so you can input a tooltip
        Button b;

        b = addCheckBox(appearanceComposite, "Enable link on automatic parenthesis or literals closing", AUTO_LINK, 0);
        b.setToolTipText(WrapAndCaseUtils.wrap(
                "Enabling this option will enable the linking mode after a parenthesis or literal is auto-closed.",
                TOOLTIP_WIDTH));

        //auto par
        b = addCheckBox(appearanceComposite, "Automatic parentheses insertion", AUTO_PAR, 0);
        b.setToolTipText(WrapAndCaseUtils.wrap("Enabling this option will enable automatic insertion of parentheses.  "
                + "Specifically, whenever you hit a brace such as '(', '{', or '[', its related peer will be inserted "
                + "and your cursor will be placed between the two braces.", TOOLTIP_WIDTH));

        //indent
        b = addCheckBox(appearanceComposite, "After '(' indent to its level (indents by tabs if unchecked)",
                AUTO_INDENT_TO_PAR_LEVEL, 0);
        Control c = addTextField(appearanceComposite, "Number of indentation levels to add:",
                AUTO_INDENT_AFTER_PAR_WIDTH, 3, 20, true);
        createInverseDependency(b, AUTO_INDENT_AFTER_PAR_WIDTH, c);

        //auto dedent 'else:'
        b = addCheckBox(appearanceComposite, "Automatic dedent of 'else:' and 'elif:'", AUTO_DEDENT_ELSE, 0);

        //auto braces
        b = addCheckBox(appearanceComposite, "Automatically skip matching braces when typing", AUTO_BRACES, 0);
        b.setToolTipText(WrapAndCaseUtils
                .wrap("Enabling this option will enable automatically skipping matching braces "
                        + "if you try to insert them.  For example, if you have the following code:\n\n"
                        + "def function(self):\n\n"
                        + "...with your cursor before the end parenthesis (after the 'f' in \"self\"), typing a ')' will "
                        + "simply move the cursor to the position after the ')' without inserting a new one.",
                        TOOLTIP_WIDTH));

        //smart indent
        b = addCheckBox(appearanceComposite, "Use smart-indent?", SMART_INDENT_PAR, 0);

        //auto colon
        b = addCheckBox(appearanceComposite, "Automatic colon detection", AUTO_COLON, 0);
        b.setToolTipText(WrapAndCaseUtils
                .wrap("Enabling this feature will enable the editor to detect if you are trying "
                        + "to enter a colon which is already there.  Instead of inserting another colon, the editor will "
                        + "simply move your cursor to the next position after the colon.", TOOLTIP_WIDTH));

        //auto literals
        b = addCheckBox(appearanceComposite, "Automatic literal closing", AUTO_LITERALS, 0);
        b.setToolTipText(WrapAndCaseUtils.wrap("Automatically close literals "
                + "(when ' or \" is added, another one is added to close it).", TOOLTIP_WIDTH));

        //auto import str
        b = addCheckBox(appearanceComposite, "Automatic insertion of the 'import' string on 'from xxx' ",
                AUTO_WRITE_IMPORT_STR, 0);
        b.setToolTipText(WrapAndCaseUtils.wrap("Enabling this will allow the editor to automatically write the"
                + "'import' string when you write a space after you've written 'from xxx '.", TOOLTIP_WIDTH));

        addCheckBox(appearanceComposite, "Add 'self' automatically when declaring methods?", AUTO_ADD_SELF, 0);

        KeySequence down = KeyBindingHelper.getCommandKeyBinding(ITextEditorActionDefinitionIds.MOVE_LINES_DOWN);
        KeySequence up = KeyBindingHelper.getCommandKeyBinding(ITextEditorActionDefinitionIds.MOVE_LINES_UP);
        String downKey = down != null ? down.format() : "Alt+Down"; //set the default if not there
        String upKey = up != null ? up.format() : "Alt+Up"; //set the default if not there
        addCheckBox(appearanceComposite,
                StringUtils.format("Smart move for line up  (%s) and line down (%s)?.", upKey, downKey),
                SMART_LINE_MOVE, 0);

        addLabel(appearanceComposite, "Note: smart move line up/down change applied on editor restart.", 20);
        return appearanceComposite;
    }
}
