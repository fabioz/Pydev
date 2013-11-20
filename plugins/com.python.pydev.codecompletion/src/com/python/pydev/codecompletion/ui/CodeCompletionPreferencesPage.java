/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.python.pydev.codecompletion.ui;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;

import com.python.pydev.codecompletion.CodeCompletionPreferencesInitializer;
import com.python.pydev.codecompletion.CodecompletionPlugin;
import com.python.pydev.codecompletion.simpleassist.KeywordsSimpleAssist;

public class CodeCompletionPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public CodeCompletionPreferencesPage() {
        super(FLAT);
        setDescription("PyDev Code Completion");
        setPreferenceStore(null);
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return CodecompletionPlugin.getDefault().getPreferenceStore();
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new IntegerFieldEditor(
                CodeCompletionPreferencesInitializer.CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION,
                "Number of chars for showing modules in context-insensitive completions?", p));

        addField(new IntegerFieldEditor(
                CodeCompletionPreferencesInitializer.CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION,
                "Number of chars for showing global tokens in context-insensitive completions?", p));

        addField(new BooleanFieldEditor(CodeCompletionPreferencesInitializer.USE_KEYWORDS_CODE_COMPLETION,
                "Use common tokens auto code completion?", p));
        addField(new LabelFieldEditor("LabelFieldEditor", "", p));

        addField(new BooleanFieldEditor(CodeCompletionPreferencesInitializer.ADD_SPACE_WHEN_NEEDED,
                "Add <SPACE> for common cases (e.g.: \"and \", \"assert \", etc.)?", p));
        addField(new LabelFieldEditor("LabelFieldEditor", "", p));

        addField(new BooleanFieldEditor(CodeCompletionPreferencesInitializer.ADD_SPACE_AND_COLON_WHEN_NEEDED,
                "Add <SPACE><COLON> for common cases (e.g.: \"class :\", \"if :\", etc.)?", p));
        addField(new LabelFieldEditor("LabelFieldEditor", "", p));

        addField(new BooleanFieldEditor(CodeCompletionPreferencesInitializer.FORCE_PY3K_PRINT_ON_PY2,
                "Force print() function on Python 2.x projects?", p));
        addField(new LabelFieldEditor("LabelFieldEditor", "", p));

        addField(new ListEditor(CodeCompletionPreferencesInitializer.KEYWORDS_CODE_COMPLETION, "Tokens to use:", p) {

            @Override
            protected String createList(String[] items) {
                return KeywordsSimpleAssist.wordsAsString(items);
            }

            @Override
            protected String getNewInputObject() {
                InputDialog d = new InputDialog(getShell(), "New word", "Add the word you wish.", "",
                        new IInputValidator() {

                            public String isValid(String newText) {
                                if (newText.indexOf(' ') != -1) {
                                    return "The input cannot have spaces";
                                }
                                return null;
                            }
                        });

                int retCode = d.open();
                if (retCode == InputDialog.OK) {
                    return d.getValue();
                }
                return null;
            }

            @Override
            protected String[] parseString(String stringList) {
                return KeywordsSimpleAssist.stringAsWords(stringList);
            }

            @Override
            protected void doFillIntoGrid(Composite parent, int numColumns) {
                super.doFillIntoGrid(parent, numColumns);
                List listControl = getListControl(parent);
                GridData layoutData = (GridData) listControl.getLayoutData();
                layoutData.heightHint = 300;
            }
        });
    }

    public void init(IWorkbench workbench) {
    }

    public static int getCharsForContextInsensitiveModulesCompletion() {
        String prefName = CodeCompletionPreferencesInitializer.CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION;
        return getIntFromPrefs(prefName);
    }

    private static int getIntFromPrefs(String prefName) {
        if (SharedCorePlugin.inTestMode()) {
            return 1;
        }
        CodecompletionPlugin plugin = CodecompletionPlugin.getDefault();
        return plugin.getPreferenceStore().getInt(prefName);
    }

    public static int getCharsForContextInsensitiveGlobalTokensCompletion() {
        String prefName = CodeCompletionPreferencesInitializer.CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION;
        return getIntFromPrefs(prefName);
    }

    public static boolean useKeywordsCodeCompletion() {
        return CodecompletionPlugin.getDefault().getPreferenceStore()
                .getBoolean(CodeCompletionPreferencesInitializer.USE_KEYWORDS_CODE_COMPLETION);
    }

    public static boolean addSpaceWhenNeeded() {
        return CodecompletionPlugin.getDefault().getPreferenceStore()
                .getBoolean(CodeCompletionPreferencesInitializer.ADD_SPACE_WHEN_NEEDED);
    }

    public static boolean addSpaceAndColonWhenNeeded() {
        return CodecompletionPlugin.getDefault().getPreferenceStore()
                .getBoolean(CodeCompletionPreferencesInitializer.ADD_SPACE_AND_COLON_WHEN_NEEDED);
    }

    public static boolean forcePy3kPrintOnPy2() {
        return CodecompletionPlugin.getDefault().getPreferenceStore()
                .getBoolean(CodeCompletionPreferencesInitializer.FORCE_PY3K_PRINT_ON_PY2);
    }

    public static String[] getKeywords() {
        String keywords = CodecompletionPlugin.getDefault().getPreferenceStore()
                .getString(CodeCompletionPreferencesInitializer.KEYWORDS_CODE_COMPLETION);
        return KeywordsSimpleAssist.stringAsWords(keywords);
    }

}
