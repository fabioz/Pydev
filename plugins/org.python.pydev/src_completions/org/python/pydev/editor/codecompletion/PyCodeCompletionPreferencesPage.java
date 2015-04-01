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

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;

/**
 * The preferences for autocompletion should only be reactivated when the code completion feature gets better (more stable and precise).
 *
 * @author Fabio Zadrozny
 */
public class PyCodeCompletionPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_CODECOMPLETION = "USE_CODECOMPLETION";
    public static final boolean DEFAULT_USE_CODECOMPLETION = true;

    public static final String ATTEMPTS_CODECOMPLETION = "ATTEMPTS_CODECOMPLETION";
    public static final int DEFAULT_ATTEMPTS_CODECOMPLETION = 5;

    public static final String AUTOCOMPLETE_ON_DOT = "AUTOCOMPLETE_ON_DOT";
    public static final boolean DEFAULT_AUTOCOMPLETE_ON_DOT = true;

    public static final String MAX_MILLIS_FOR_COMPLETION = "MAX_MILLIS_FOR_COMPLETION";
    public static final int DEFAULT_MAX_MILLIS_FOR_COMPLETION = 5 * 1000; //Default is 5 seconds

    public static final String AUTOCOMPLETE_ON_ALL_ASCII_CHARS = "AUTOCOMPLETE_ON_ALL_ASCII_CHARS";
    public static final boolean DEFAULT_AUTOCOMPLETE_ON_ALL_ASCII_CHARS = true;

    public static final String USE_AUTOCOMPLETE = "USE_AUTOCOMPLETE";
    public static final boolean DEFAULT_USE_AUTOCOMPLETE = true;

    public static final String AUTOCOMPLETE_DELAY = "AUTOCOMPLETE_DELAY";
    public static final int DEFAULT_AUTOCOMPLETE_DELAY = 0;

    public static final String AUTOCOMPLETE_ON_PAR = "AUTOCOMPLETE_ON_PAR";
    public static final boolean DEFAULT_AUTOCOMPLETE_ON_PAR = false;

    public static final String APPLY_COMPLETION_ON_DOT = "APPLY_COMPLETION_ON_DOT";
    public static final boolean DEFAULT_APPLY_COMPLETION_ON_DOT = false;

    public static final String APPLY_COMPLETION_ON_LPAREN = "APPLY_COMPLETION_ON_LPAREN";
    public static final boolean DEFAULT_APPLY_COMPLETION_ON_LPAREN = false;

    public static final String APPLY_COMPLETION_ON_RPAREN = "APPLY_COMPLETION_ON_RPAREN";
    public static final boolean DEFAULT_APPLY_COMPLETION_ON_RPAREN = false;

    public static final String ARGUMENTS_DEEP_ANALYSIS_N_CHARS = "DEEP_ANALYSIS_N_CHARS";
    public static final int DEFAULT_ARGUMENTS_DEEP_ANALYSIS_N_CHARS = 1;

    public static final String USE_CODE_COMPLETION_ON_DEBUG_CONSOLES = "USE_CODE_COMPLETION_ON_DEBUG_CONSOLES";
    public static final boolean DEFAULT_USE_CODE_COMPLETION_ON_DEBUG_CONSOLES = true;

    /**
     */
    public PyCodeCompletionPreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new IntegerFieldEditor(ATTEMPTS_CODECOMPLETION,
                "Maximum attempts to connect to shell (5 secs each):", p));

        addField(new IntegerFieldEditor(AUTOCOMPLETE_DELAY, "Autocompletion delay: ", p));

        String tooltip = WrapAndCaseUtils
                .wrap("Determines the number of chars in the qualifier request "
                        + "for which constructs such as 'from xxx import yyy' should be "
                        + "analyzed to get its actual token and if it maps to a method, its paramaters will be added in the completion.",
                        80);
        IntegerFieldEditor deepAnalysisFieldEditor = new IntegerFieldEditor(ARGUMENTS_DEEP_ANALYSIS_N_CHARS,
                "Minimum number of chars in qualifier for\ndeep analysis for parameters in 'from' imports:", p);
        addField(deepAnalysisFieldEditor);
        deepAnalysisFieldEditor.getLabelControl(p).setToolTipText(tooltip);
        deepAnalysisFieldEditor.getTextControl(p).setToolTipText(tooltip);

        addField(new BooleanFieldEditor(USE_CODECOMPLETION, "Use code completion?", p));

        addField(new IntegerFieldEditor(
                MAX_MILLIS_FOR_COMPLETION,
                "Maximum millis for a code-completion request to complete?", p));

        addField(new BooleanFieldEditor(USE_CODE_COMPLETION_ON_DEBUG_CONSOLES,
                "Use code completion on debug console sessions?", p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_CODE_COMPLETION_DEBUG_CONSOLE",
                "Note: only applied for new consoles.", p));

        addField(new BooleanFieldEditor(AUTOCOMPLETE_ON_DOT, "Request completion on '.'?", p));

        addField(new BooleanFieldEditor(AUTOCOMPLETE_ON_PAR, "Request completion on '('?", p));

        addField(new BooleanFieldEditor(AUTOCOMPLETE_ON_ALL_ASCII_CHARS,
                "Request completion on all letter chars and '_'?", p));

        addField(new BooleanFieldEditor(APPLY_COMPLETION_ON_DOT, "Apply completion on '.'?", p));

        addField(new BooleanFieldEditor(APPLY_COMPLETION_ON_LPAREN, "Apply completion on '('?", p));

        addField(new BooleanFieldEditor(APPLY_COMPLETION_ON_RPAREN, "Apply completion on ')'?", p));

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
                        + "erasing the next chars from the current token.", p));

        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_2a", "", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_2b", "", p));
        addField(new LabelFieldEditor("LABEL_FIELD_EDITOR_NEW_LINE_ALWAYS_THERE_2c", "", p));

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    public static boolean useCodeCompletion() {
        return getPreferences().getBoolean(PyCodeCompletionPreferencesPage.USE_CODECOMPLETION);
    }

    public static boolean useCodeCompletionOnDebug() {
        return getPreferences().getBoolean(PyCodeCompletionPreferencesPage.USE_CODE_COMPLETION_ON_DEBUG_CONSOLES);
    }

    public static int getNumberOfConnectionAttempts() {
        if (SharedCorePlugin.inTestMode()) {
            return 20;
        }

        Preferences preferences = getPreferences();
        int ret = preferences.getInt(PyCodeCompletionPreferencesPage.ATTEMPTS_CODECOMPLETION);
        if (ret < 2) {
            ret = 2; // at least 2 attempts!
        }
        return ret;
    }

    public static int getMaximumNumberOfMillisToCompleteCodeCompletionRequest() {
        int val = getPreferences().getInt(PyCodeCompletionPreferencesPage.MAX_MILLIS_FOR_COMPLETION);
        if (val <= 200) {
            //Never less than 200 millis
            val = 200;
        }
        if (val >= 120 * 1000) {
            //Never more than 2 minutes
            val = 120 * 1000;
        }
        return val;
    }

    public static boolean isToAutocompleteOnDot() {
        return getPreferences().getBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_DOT);
    }

    public static boolean isToAutocompleteOnPar() {
        return getPreferences().getBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_PAR);
    }

    public static boolean useAutocomplete() {
        return getPreferences().getBoolean(PyCodeCompletionPreferencesPage.USE_AUTOCOMPLETE);
    }

    public static boolean useAutocompleteOnAllAsciiChars() {
        return getPreferences().getBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_ALL_ASCII_CHARS);
    }

    public static int getAutocompleteDelay() {
        return getPreferences().getInt(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_DELAY);
    }

    public static int getArgumentsDeepAnalysisNChars() {
        if (SharedCorePlugin.inTestMode()) {
            return 0;
        }
        return getPreferences().getInt(PyCodeCompletionPreferencesPage.ARGUMENTS_DEEP_ANALYSIS_N_CHARS);
    }

    public static boolean applyCompletionOnDot() {
        return getPreferences().getBoolean(PyCodeCompletionPreferencesPage.APPLY_COMPLETION_ON_DOT);
    }

    public static boolean applyCompletionOnLParen() {
        return getPreferences().getBoolean(PyCodeCompletionPreferencesPage.APPLY_COMPLETION_ON_LPAREN);
    }

    public static boolean applyCompletionOnRParen() {
        return getPreferences().getBoolean(PyCodeCompletionPreferencesPage.APPLY_COMPLETION_ON_RPAREN);
    }

    private static Preferences getPreferences() {
        if (SharedCorePlugin.inTestMode()) {
            //always create a new one for tests.
            return getPreferencesForTests.call(null);
        }
        PydevPlugin plugin = PydevPlugin.getDefault();
        return plugin.getPluginPreferences();
    }

    public static ICallback<Preferences, Object> getPreferencesForTests;

}