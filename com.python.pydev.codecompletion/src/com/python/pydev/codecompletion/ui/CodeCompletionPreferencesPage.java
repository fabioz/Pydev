/*
 * Created on 24/09/2005
 */
package com.python.pydev.codecompletion.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.python.pydev.codecompletion.CodeCompletionPreferencesInitializer;
import com.python.pydev.codecompletion.CodecompletionPlugin;

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

        addField(new BooleanFieldEditor(CodeCompletionPreferencesInitializer.USE_KEYWORDS_CODE_COMPLETION, "Use keywords (and common tokens) auto code completion?", p));
    }

    public void init(IWorkbench workbench) {
    }

    public static boolean useKeywordsCodeCompletion(){
        return CodecompletionPlugin.getDefault().getPreferenceStore().getBoolean(CodeCompletionPreferencesInitializer.USE_KEYWORDS_CODE_COMPLETION);
    }

}
