package org.python.pydev.plugin.preferences;

import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.StringUtils;

public class FileTypesPreferences {

    public static final String VALID_SOURCE_FILES = "VALID_SOURCE_FILES";
    public final static String DEFAULT_VALID_SOURCE_FILES = "py, pyw, pyx, pxd, pxi";

    public static final String FIRST_CHOICE_PYTHON_SOURCE_FILE = "FIRST_CHOICE_PYTHON_SOURCE_FILE";
    public final static String DEFAULT_FIRST_CHOICE_PYTHON_SOURCE_FILE = "py";

    /**
     * Helper to keep things cached as needed (so that we don't have to get it from the cache all the time.
     *
     * @author Fabio
     */
    private static class PreferencesCacheHelper implements IPreferenceChangeListener {
        private static PreferencesCacheHelper singleton;

        static synchronized PreferencesCacheHelper get() {
            if (singleton == null) {
                singleton = new PreferencesCacheHelper();
            }
            return singleton;
        }

        public PreferencesCacheHelper() {
            PydevPrefs.getEclipsePreferences().addPreferenceChangeListener(this);
        }

        @Override
        public void preferenceChange(PreferenceChangeEvent event) {
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
                String validStr = PydevPrefs.getEclipsePreferences().get(VALID_SOURCE_FILES,
                        DEFAULT_VALID_SOURCE_FILES);
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
        return "." + PydevPrefs.getEclipsePreferences().get(FIRST_CHOICE_PYTHON_SOURCE_FILE,
                DEFAULT_FIRST_CHOICE_PYTHON_SOURCE_FILE);
    }

    public static String[] getWildcardJythonValidZipFiles() {
        return new String[] { "*.jar", "*.zip" };
    }

    public static String[] getWildcardPythonValidZipFiles() {
        return new String[] { "*.egg", "*.zip" };
    }

    public static boolean isCythonFile(String name) {
        return name != null && (name.endsWith(".pyx") || name.endsWith(".pxd") || name.endsWith(".pxi"));
    }

}
