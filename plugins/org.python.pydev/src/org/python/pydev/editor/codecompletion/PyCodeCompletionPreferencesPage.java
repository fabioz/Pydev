/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Aug 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;

/**
 * The preferences for autocompletion should only be reactivated when the code completion feature gets better (more stable and precise).
 *
 * @author Fabio Zadrozny
 */
public class PyCodeCompletionPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     */
    public PyCodeCompletionPreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPrefs.getPreferenceStore());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new IntegerFieldEditor(PyCodeCompletionPreferences.ATTEMPTS_CODECOMPLETION,
                "Maximum attempts to connect to shell (5 secs each):", p));

        addField(new IntegerFieldEditor(PyCodeCompletionPreferences.AUTOCOMPLETE_DELAY, "Autocompletion delay: ", p));

        String tooltip = WrapAndCaseUtils
                .wrap("Determines the number of chars in the qualifier request "
                        + "for which constructs such as 'from xxx import yyy' should be "
                        + "analyzed to get its actual token and if it maps to a method, its paramaters will be added in the completion.",
                        80);
        IntegerFieldEditor deepAnalysisFieldEditor = new IntegerFieldEditor(
                PyCodeCompletionPreferences.ARGUMENTS_DEEP_ANALYSIS_N_CHARS,
                "Minimum number of chars in qualifier for\ndeep analysis for parameters in 'from' imports:", p);
        addField(deepAnalysisFieldEditor);
        deepAnalysisFieldEditor.getLabelControl(p).setToolTipText(tooltip);
        deepAnalysisFieldEditor.getTextControl(p).setToolTipText(tooltip);

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.USE_CODECOMPLETION, "Use code completion?", p));

        addField(new IntegerFieldEditor(
                PyCodeCompletionPreferences.MAX_MILLIS_FOR_COMPLETION,
                "Maximum millis for a code-completion request to complete?", p));

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.USE_CODE_COMPLETION_ON_DEBUG_CONSOLES,
                "Use code completion on debug console sessions?", p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_CODE_COMPLETION_DEBUG_CONSOLE",
                "Note: only applied for new consoles.", p));

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.AUTOCOMPLETE_ON_DOT, "Request completion on '.'?",
                p));

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.AUTOCOMPLETE_ON_PAR, "Request completion on '('?",
                p));

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.AUTOCOMPLETE_ON_ALL_ASCII_CHARS,
                "Request completion on all letter chars and '_'?", p));

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.MATCH_BY_SUBSTRING_IN_CODE_COMPLETION,
                "Match substrings on code completion?", p));

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.APPLY_COMPLETION_ON_DOT, "Apply completion on '.'?",
                p));

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.APPLY_COMPLETION_ON_LPAREN,
                "Apply completion on '('?", p));

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.APPLY_COMPLETION_ON_RPAREN,
                "Apply completion on ')'?", p));

        addField(new BooleanFieldEditor(PyCodeCompletionPreferences.PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD,
                "Put local imports on top of method?", p));
        addField(new LabelFieldEditor("LABEL_PUT_LOCAL_IMPORTS_IN_TOP_OF_METHOD_ALWAYS_THERE_0",
                "Note: in a code-completion with a local auto-import.", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_a", "", p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_a", "", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_b", "", p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_0",
                "Note: ENTER will always apply the completion.", p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_0a", "", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_0b", "", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_0c", "", p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_1",
                "Note 2: Shift + ENTER can be used if you want a new line\n" + "without applying a completion.", p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_1a", "", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_1b", "", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_1c", "", p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_2",
                "Note 3: Ctrl + ENTER can be used as a way to apply the completion\n"
                        + "erasing the next chars from the current token.",
                p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_2a", "", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_2b", "", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_2c", "", p));

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
    }

}