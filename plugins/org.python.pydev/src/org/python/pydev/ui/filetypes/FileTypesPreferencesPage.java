/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.filetypes;

import java.util.List;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
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
    public final static String DEFAULT_VALID_SOURCE_FILES = "py, pyw, pyx, pxd, pxi";

    public static final String FIRST_CHOICE_PYTHON_SOURCE_FILE = "FIRST_CHOICE_PYTHON_SOURCE_FILE";
    public final static String DEFAULT_FIRST_CHOICE_PYTHON_SOURCE_FILE = "py";

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();

        addField(new LabelFieldEditorWith2Cols("Label_Info_File_Preferences1", WrapAndCaseUtils.wrap(
                "These setting are used to know which files should be considered valid internally, and are "
                        + "not used in the file association of those files to the pydev editor.\n\n", 80), p) {
            @Override
            public String getLabelTextCol1() {
                return "Note:\n\n";
            }
        });

        addField(new LabelFieldEditorWith2Cols(
                "Label_Info_File_Preferences2",
                WrapAndCaseUtils
                        .wrap("After changing those settings, a manual reconfiguration of the interpreter and a manual rebuild "
                                + "of the projects may be needed to update the inner caches that may be affected by those changes.\n\n",
                                80), p) {
            @Override
            public String getLabelTextCol1() {
                return "Important:\n\n";
            }
        });

        addField(new StringFieldEditor(VALID_SOURCE_FILES, "Valid source files (comma-separated):",
                StringFieldEditor.UNLIMITED, p));
        addField(new StringFieldEditor(FIRST_CHOICE_PYTHON_SOURCE_FILE, "Default python extension:",
                StringFieldEditor.UNLIMITED, p));
    }

    public void init(IWorkbench workbench) {
        // pass
    }

    /**
     * Helper to keep things cached as needed (so that we don't have to get it from the cache all the time.
     *
     * @author Fabio
     */
    private static class PreferencesCacheHelper implements IPropertyChangeListener {
        private static PreferencesCacheHelper singleton;

        static synchronized PreferencesCacheHelper get() {
            if (singleton == null) {
                singleton = new PreferencesCacheHelper();
            }
            return singleton;
        }

        public PreferencesCacheHelper() {
            PydevPrefs.getPreferences().addPropertyChangeListener(this);
        }

        public void propertyChange(PropertyChangeEvent event) {
            this.wildcaldValidSourceFiles = null;
            this.dottedValidSourceFiles = null;
            this.pythondValidSourceFiles = null;
            this.pythonValidInitFiles = null;
        }

        //return new String[] { "*.py", "*.pyw" };
        private String[] wildcaldValidSourceFiles;

        public String[] getCacheWildcardValidSourceFiles() {
            String[] ret = wildcaldValidSourceFiles;
            if (ret == null) {
                String[] validSourceFiles = this.getCacheValidSourceFiles();
                String[] s = new String[validSourceFiles.length];
                for (int i = 0; i < validSourceFiles.length; i++) {
                    s[i] = "*." + validSourceFiles[i];
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
            if (ret == null) {
                String[] validSourceFiles = this.getCacheValidSourceFiles();
                String[] s = new String[validSourceFiles.length];
                for (int i = 0; i < validSourceFiles.length; i++) {
                    s[i] = "." + validSourceFiles[i];
                }
                dottedValidSourceFiles = s;
                ret = s;
            }
            return ret;
        }

        //return new String[] { "py", "pyw" };
        private String[] pythondValidSourceFiles;

        /**
         * __init__.py, __init__.pyw, etc...
         */
        private String[] pythonValidInitFiles;

        public String[] getCacheValidInitFiles() {
            String[] ret = pythonValidInitFiles;
            if (ret == null) {
                String[] cacheValidSourceFiles = getCacheValidSourceFiles();
                ret = new String[cacheValidSourceFiles.length];
                for (int i = 0; i < ret.length; i++) {
                    ret[i] = "__init__." + cacheValidSourceFiles[i];
                }
                pythonValidInitFiles = ret;
            }
            return ret;
        }

        public String[] getCacheValidSourceFiles() {
            String[] ret = pythondValidSourceFiles;
            if (ret == null) {
                String validStr = PydevPrefs.getPreferences().getString(FileTypesPreferencesPage.VALID_SOURCE_FILES);
                final List<String> temp = StringUtils.splitAndRemoveEmptyTrimmed(validStr, ',');
                String[] s = temp.toArray(new String[temp.size()]);
                for (int i = 0; i < s.length; i++) {
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
        if (path.endsWith(".pyd") || path.endsWith(".so") || path.endsWith(".dll") || path.endsWith(".a")) {
            return true;
        }
        return false;
    }

    /**
     * @param extension extension we want to analyze
     * @return if the extension passed belongs to a valid python compiled extension
     */
    public static boolean isValidDllExtension(String extension) {
        if (extension.equals("pyd") || extension.equals("so") || extension.equals("dll") || extension.equals("a")) {
            return true;
        }
        return false;
    }

    public final static String getDefaultDottedPythonExtension() {
        return "." + PydevPrefs.getPreferences().getString(FIRST_CHOICE_PYTHON_SOURCE_FILE);
    }

    public static String[] getWildcardJythonValidZipFiles() {
        return new String[] { "*.jar", "*.zip" };
    }

    public static String[] getWildcardPythonValidZipFiles() {
        return new String[] { "*.egg", "*.zip" };
    }

    // items that are customizable -- things gotten from the cache -----------------------------------------------------

    public static String[] getWildcardValidSourceFiles() {
        if (SharedCorePlugin.inTestMode()) {
            return new String[] { "*.py", "*.pyw", "*.pyx", "*.pxd", "*.pxi" };
        }

        return PreferencesCacheHelper.get().getCacheWildcardValidSourceFiles();
    }

    public final static String[] getDottedValidSourceFiles() {
        if (SharedCorePlugin.inTestMode()) {
            return new String[] { ".py", ".pyw", ".pyx", ".pxd", ".pxi" };
        }

        return PreferencesCacheHelper.get().getCacheDottedValidSourceFiles();
    }

    public final static String[] getValidSourceFiles() {
        if (SharedCorePlugin.inTestMode()) {
            return new String[] { "py", "pyw", "pyx", "pxd", "pxi" };
        }

        return PreferencesCacheHelper.get().getCacheValidSourceFiles();
    }

    public final static String[] getValidInitFiles() {
        if (SharedCorePlugin.inTestMode()) {
            return new String[] { "__init__.py", "__init__.pyw", "__init__.pyx", "__init__.pxd", "__init__.pxi" };
        }

        return PreferencesCacheHelper.get().getCacheValidInitFiles();
    }

    public static boolean isCythonFile(String name) {
        return name != null && (name.endsWith(".pyx") || name.endsWith(".pxd") || name.endsWith(".pxi"));
    }
}
