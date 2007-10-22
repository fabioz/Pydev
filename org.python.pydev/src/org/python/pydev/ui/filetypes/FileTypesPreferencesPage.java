package org.python.pydev.ui.filetypes;

import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.docutils.WordUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.utils.LabelFieldEditorWith2Cols;

/**
 * Preferences regarding the python file types available.
 * 
 * Also provides a better access to them and caches to make that access efficient.
 *
 * @author Fabio
 */
public class FileTypesPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {


    public FileTypesPreferencesPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("File Types Preferences");
    }

    public static final String VALID_SOURCE_FILES = "VALID_SOURCE_FILES";
    public final static String DEFAULT_VALID_SOURCE_FILES = "py, pyw";
    
    public static final String FIRST_CHOICE_PYTHON_SOURCE_FILE = "FIRST_CHOICE_PYTHON_SOURCE_FILE";
    public final static String DEFAULT_FIRST_CHOICE_PYTHON_SOURCE_FILE = "py";

    
    
    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();
        
        addField(new LabelFieldEditorWith2Cols("Label_Info_File_Preferences1", 
                WordUtils.wrap("These setting are used to know which files should be considered valid internally, and are " +
                "not used in the file association of those files to the pydev editor.\n\n", 80), 
                p){
                    @Override
                    public String getLabelTextCol1() {
                        return "Note:\n\n";
                    }
        });
        
        
        addField(new LabelFieldEditorWith2Cols("Label_Info_File_Preferences2", 
                WordUtils.wrap("After changing those settings, a manual reconfiguration of the interpreter and a manual rebuild " +
                "of the projects may be needed to update the inner caches that may be affected by those changes.\n\n", 80), 
                p){
                    @Override
                    public String getLabelTextCol1() {
                        return "Important:\n\n";
                    }
        });
        
        
        addField(new StringFieldEditor(VALID_SOURCE_FILES, "Valid source files (comma-separated):", StringFieldEditor.UNLIMITED, p));
        addField(new StringFieldEditor(FIRST_CHOICE_PYTHON_SOURCE_FILE, "Default python extension:", StringFieldEditor.UNLIMITED, p));
    }
    
    
    public void init(IWorkbench workbench) {
        // pass
    }
    
    
    /**
     * Helper to keep things cached as needed (so that we don't have to get it from the cache all the time.
     *
     * @author Fabio
     */
    private static class PreferencesCacheHelper implements IPropertyChangeListener{
        private static PreferencesCacheHelper singleton;

        static synchronized PreferencesCacheHelper get(){
            if(singleton == null){
                singleton = new PreferencesCacheHelper();
            }
            return singleton;
        }
        
        public PreferencesCacheHelper(){
            PydevPrefs.getPreferences().addPropertyChangeListener(this);
        }

        public void propertyChange(PropertyChangeEvent event) {
            this.wildcaldValidSourceFiles = null;
            this.dottedValidSourceFiles = null;
            this.pythondValidSourceFiles = null;
        }
        

        //return new String[] { "*.py", "*.pyw" };
        private String[] wildcaldValidSourceFiles;
        public String[] getCacheWildcardValidSourceFiles() {
            String[] ret = wildcaldValidSourceFiles;
            if(ret == null){
                String[] validSourceFiles = this.getCacheValidSourceFiles();
                String[] s = new String[validSourceFiles.length];
                for (int i=0;i<validSourceFiles.length;i++) {
                    s[i] = "*."+validSourceFiles[i];
                }
                wildcaldValidSourceFiles = s;
                ret = s;
            }
            return ret;
        }

        //return new String[] { ".py", ".pyw" };
        private String[] dottedValidSourceFiles;
        public String[] getCacheDottedValidSourceFiles() {
            String[] ret = dottedValidSourceFiles;
            if(ret == null){
                String[] validSourceFiles = this.getCacheValidSourceFiles();
                String[] s = new String[validSourceFiles.length];
                for (int i=0;i<validSourceFiles.length;i++) {
                    s[i] = "."+validSourceFiles[i];
                }
                dottedValidSourceFiles = s;
                ret = s;
            }
            return ret;
        }

        //return new String[] { "py", "pyw" };
        private String[] pythondValidSourceFiles;
        public String[] getCacheValidSourceFiles() {
            String[] ret = pythondValidSourceFiles;
            if(ret == null){
                String validStr = PydevPrefs.getPreferences().getString(FileTypesPreferencesPage.VALID_SOURCE_FILES);
                String[] s = FullRepIterable.split(validStr, ',');
                for(int i=0;i<s.length;i++){
                    s[i] = s[i].trim();
                }
                pythondValidSourceFiles = s;
                ret = s;
            }
            return ret;
        }
    }
    
    
    
    // public interface with the hardcoded settings --------------------------------------------------------------------
    

    /**
     * @return true if the given filename should be considered a zip file.
     */
    public static boolean isValidZipFile(String fileName) {
        return fileName.endsWith(".jar") || fileName.endsWith(".zip") || fileName.endsWith(".egg");
    }


    /**
     * @param path the path we want to analyze
     * @return if the path passed belongs to a valid python compiled extension
     */
    public static boolean isValidDll(String path) {
        if (path.endsWith(".pyd") || path.endsWith(".so") || path.endsWith(".dll")) {
            return true;
        }
        return false;
    }


    public final static String getDefaultDottedPythonExtension() {
        return "."+PydevPrefs.getPreferences().getString(FIRST_CHOICE_PYTHON_SOURCE_FILE);
    }

    public static String[] getWildcardJythonValidZipFiles(){
        return new String[] { "*.jar", "*.zip" };
    }


    public static String[] getWildcardPythonValidZipFiles(){
        return new String[] { "*.egg", "*.zip" };
    }


    
    
    
    // items that are customizable -- things gotten from the cache -----------------------------------------------------
    
    
    public static String[] getWildcardValidSourceFiles(){
        try {
            return PreferencesCacheHelper.get().getCacheWildcardValidSourceFiles();
        } catch (NullPointerException e) {
            return new String[]{"*.py", "*.pyw"}; // in tests
        }
    }


    public final static String[] getDottedValidSourceFiles() {
        try {
            return PreferencesCacheHelper.get().getCacheDottedValidSourceFiles();
        } catch (NullPointerException e) {
            return new String[]{".py", ".pyw"}; // in tests
        }
    }

    
    public final static String[] getValidSourceFiles() {
        try {
            return PreferencesCacheHelper.get().getCacheValidSourceFiles();
        } catch (NullPointerException e) {
            return new String[]{"py", "pyw"}; // in tests
        }
    }

    
}
