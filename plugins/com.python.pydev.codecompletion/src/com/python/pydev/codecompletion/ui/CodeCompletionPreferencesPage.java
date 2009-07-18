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

        addField(new IntegerFieldEditor(CodeCompletionPreferencesInitializer.CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION, 
                "Number of chars for showing modules in context-insensitive completions?", p));
        
        addField(new IntegerFieldEditor(CodeCompletionPreferencesInitializer.CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION, 
                "Number of chars for showing global tokens in context-insensitive completions?", p));
        
        addField(new BooleanFieldEditor(CodeCompletionPreferencesInitializer.USE_KEYWORDS_CODE_COMPLETION, 
                "Use common tokens auto code completion?", p));
        addField(new ListEditor(CodeCompletionPreferencesInitializer.KEYWORDS_CODE_COMPLETION, "Tokens to use:", p){

                @Override
                protected String createList(String[] items) {
                    return KeywordsSimpleAssist.wordsAsString(items);
                }
    
                @Override
                protected String getNewInputObject() {
                    InputDialog d = new InputDialog(getShell(), "New word", "Add the word you wish.", "", new IInputValidator(){
    
                        public String isValid(String newText) {
                            if(newText.indexOf(' ') != -1){
                                return "The input cannot have spaces";
                            }
                            return null;
                        }});
    
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

    public static int getCharsForContextInsensitiveModulesCompletion(){
        String prefName = CodeCompletionPreferencesInitializer.CHARS_FOR_CTX_INSENSITIVE_MODULES_COMPLETION;
        return getIntFromPrefs(prefName);
    }

    private static int getIntFromPrefs(String prefName){
        CodecompletionPlugin plugin = CodecompletionPlugin.getDefault();
        if(plugin == null){
            return 1;
        }
        return plugin.getPreferenceStore().getInt(prefName);
    }
    
    public static int getCharsForContextInsensitiveGlobalTokensCompletion(){
        String prefName = CodeCompletionPreferencesInitializer.CHARS_FOR_CTX_INSENSITIVE_TOKENS_COMPLETION;
        return getIntFromPrefs(prefName);
    }
    
    public static boolean useKeywordsCodeCompletion(){
        return CodecompletionPlugin.getDefault().getPreferenceStore().getBoolean(
                CodeCompletionPreferencesInitializer.USE_KEYWORDS_CODE_COMPLETION);
    }

    public static String[] getKeywords() {
        String keywords= CodecompletionPlugin.getDefault().getPreferenceStore().getString(
                CodeCompletionPreferencesInitializer.KEYWORDS_CODE_COMPLETION);
        return KeywordsSimpleAssist.stringAsWords(keywords);
    }

}
