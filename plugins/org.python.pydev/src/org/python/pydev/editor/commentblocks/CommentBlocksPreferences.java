/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.commentblocks;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.FontUtils;
import org.python.pydev.shared_ui.IFontUsage;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;

public class CommentBlocksPreferences extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private StringFieldEditor multiBlock;
    private StringFieldEditor singleBlock;
    private BooleanFieldEditor alignSingle;
    private Label labelSep0;
    private Label labelMulti;
    private Label labelSingle;
    private Label labelSep1;

    public CommentBlocksPreferences() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Comment Preferences");
    }

    public static final String MULTI_BLOCK_COMMENT_CHAR = "MULTI_BLOCK_COMMENT_CHAR";
    public static final String DEFAULT_MULTI_BLOCK_COMMENT_CHAR = "=";

    public static final String MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME = "MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME";
    public static final boolean DEFAULT_MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME = true;

    public static final String MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME = "MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME";
    public static final boolean DEFAULT_MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME = true;

    public static final String SINGLE_BLOCK_COMMENT_CHAR = "SINGLE_BLOCK_COMMENT_CHAR";
    public static final String DEFAULT_SINGLE_BLOCK_COMMENT_CHAR = "-";

    public static final String SINGLE_BLOCK_COMMENT_ALIGN_RIGHT = "SINGLE_BLOCK_COMMENT_ALIGN_RIGHT";
    public static final boolean DEFAULT_SINGLE_BLOCK_COMMENT_ALIGN_RIGHT = true;

    public static final String ADD_COMMENTS_AT_INDENT = "ADD_COMMENTS_AT_INDENT";
    public static final boolean DEFAULT_ADD_COMMENTS_AT_INDENT = true;

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(ADD_COMMENTS_AT_INDENT,
                "When commenting add '#' at the current indent?",
                p));

        labelSep0 = new Label(p, SWT.NONE);
        labelSep0.setText("       (otherwise, add '#' to the start of the line)");

        labelSep1 = new Label(p, SWT.NONE);

        multiBlock = new StringFieldEditor(MULTI_BLOCK_COMMENT_CHAR, "Multi-block char (ctrl+4):", 2, p);
        multiBlock.getTextControl(p).setTextLimit(1);
        multiBlock.setEmptyStringAllowed(false);
        addField(multiBlock);

        addField(new BooleanFieldEditor(MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME,
                "In a class name, create block only with class name above of class?", p));
        addField(new BooleanFieldEditor(MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME,
                "In a function name, create block only with function name above of function?", p));

        labelMulti = new Label(p, SWT.NONE);

        singleBlock = new StringFieldEditor(SINGLE_BLOCK_COMMENT_CHAR, "Single-block char (ctrl+shift+4):", 2, p);
        singleBlock.setEmptyStringAllowed(false);
        singleBlock.getTextControl(p).setTextLimit(1);
        addField(singleBlock);

        alignSingle = new BooleanFieldEditor(SINGLE_BLOCK_COMMENT_ALIGN_RIGHT,
                "Align text in single-block to the right?", p);
        addField(alignSingle);
        labelSingle = new Label(p, SWT.NONE);

        setLabelFont(p, labelSingle, true);
        setLabelFont(p, labelMulti, true);

        updateMulti(PyScopedPreferences.getString(MULTI_BLOCK_COMMENT_CHAR, null));
        updateSingle(PyScopedPreferences.getString(SINGLE_BLOCK_COMMENT_CHAR, null),
                PyScopedPreferences.getBoolean(SINGLE_BLOCK_COMMENT_ALIGN_RIGHT, null));
        addField(new ScopedPreferencesFieldEditor(p, SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE, this));
    }

    private void setLabelFont(Composite composite, Label label, boolean bold) {
        try {
            FontData labelFontData = FontUtils.getFontData(IFontUsage.WIDGET, true);
            if (bold) {
                labelFontData.setStyle(SWT.BOLD);
            }
            label.setFont(new Font(composite.getDisplay(), labelFontData));
        } catch (Throwable e) {
            //ignore
        }
    }

    private void updateSingle(String val, boolean alignToRight) {
        FastStringBuffer buf = new FastStringBuffer(200);
        if (val.length() == 0) {
            buf.append("Invalid");
            buf.appendN(' ', 23);
        } else {
            buf.appendN(val.charAt(0), 10); //Use only the pos 0!
            if (alignToRight) {
                buf.append(" my single block");
            } else {
                buf.insert(0, " my single block ");
            }
            buf.insert(0, '#');
        }
        labelSingle.setText("Result:\n" + buf.toString());
    }

    private void updateMulti(String val) {
        FastStringBuffer buf = new FastStringBuffer(200);
        if (val.length() == 0) {
            buf.append("Invalid");
            buf.appendN(' ', 23);
            buf.append('\n');
            buf.appendN(' ', 30);
            buf.append('\n');
            buf.appendN(' ', 30);
        } else {
            buf.append("#");
            buf.appendN(val.charAt(0), 26); //Use only the pos 0!
            buf.append("\n# my multi block");
            buf.append("\n#");
            buf.appendN(val.charAt(0), 26);
        }
        labelMulti.setText("Result:\n" + buf.toString() + "\n\n\n");
    }

    @Override
    public void init(IWorkbench workbench) {
        // pass
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (multiBlock.equals(event.getSource())) {
            updateMulti(multiBlock.getStringValue());
        } else if (singleBlock.equals(event.getSource()) || alignSingle.equals(event.getSource())) {
            updateSingle(singleBlock.getStringValue(), alignSingle.getBooleanValue());
        }
    }

}
