package com.python.pydev.analysis.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.docutils.StringUtils;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.AnalysisPreferenceInitializer;

/**
 * Preferences page indicating auto-import preferences
 * 
 * @author Fabio
 */
public class AutoImportsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public AutoImportsPreferencesPage() {
        super(FLAT);
        setDescription("Auto Imports");
        setPreferenceStore(null);
    }

    public void init(IWorkbench workbench) {
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return getPlugin().getPreferenceStore();
    }

    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(AnalysisPreferenceInitializer.DO_AUTO_IMPORT, "Do auto import?", BooleanFieldEditor.DEFAULT, p));
        
        addField(new BooleanFieldEditor(AnalysisPreferenceInitializer.DO_IGNORE_IMPORTS_STARTING_WITH_UNDER, 
                "Ignore last modules starting with '_' when doing auto-import/quick fix?", 
                BooleanFieldEditor.DEFAULT, p));
        
        addField(new BooleanFieldEditor(AnalysisPreferenceInitializer.DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS,
                "Do auto import on organize imports (Ctrl+Shift+O)?", BooleanFieldEditor.DEFAULT, p));
    }
    
    public static String removeImportsStartingWithUnderIfNeeded(String declPackageWithoutInit) {
        if(doIgnoreImportsStartingWithUnder()){
            String[] splitted = StringUtils.dotSplit(declPackageWithoutInit);
            StringBuffer buf = new StringBuffer();
            
            boolean foundStartingWithoutUnder=false;
            for (int i=splitted.length-1;i>=0;i--) {
                String s=splitted[i];
                if(!foundStartingWithoutUnder){
                    if(s.charAt(0) == '_'){
                        continue;
                    }
                    foundStartingWithoutUnder = true;
                }
                buf.insert(0, s);
                if(i != 0){
                    buf.insert(0, '.');
                }
            }
            declPackageWithoutInit = buf.toString();
        }
        return declPackageWithoutInit;
    }

    private static AnalysisPlugin getPlugin() {
        return AnalysisPlugin.getDefault();
    }
    

    public static boolean TESTS_DO_AUTO_IMPORT = true;
    public static boolean doAutoImport(){
        AnalysisPlugin plugin = getPlugin();
        if(plugin != null){
            return plugin.getPreferenceStore().getBoolean(AnalysisPreferenceInitializer.DO_AUTO_IMPORT);
        }
        return TESTS_DO_AUTO_IMPORT;
    }
    
    
    public static boolean TESTS_DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS = true;
    public static boolean doAutoImportOnOrganizeImports(){
        AnalysisPlugin plugin = getPlugin();
        if(plugin != null){
            return plugin.getPreferenceStore().getBoolean(AnalysisPreferenceInitializer.DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS);
        }
        return TESTS_DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS;
    }

    
    public static boolean TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;
    public static boolean doIgnoreImportsStartingWithUnder(){
        AnalysisPlugin plugin = getPlugin();
        if(plugin != null){
            return plugin.getPreferenceStore().getBoolean(AnalysisPreferenceInitializer.DO_IGNORE_IMPORTS_STARTING_WITH_UNDER);
        }
        return TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER;
    }

}
