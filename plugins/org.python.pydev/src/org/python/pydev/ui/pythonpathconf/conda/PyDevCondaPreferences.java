package org.python.pydev.ui.pythonpathconf.conda;

import java.io.File;

import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.PydevPrefs;

public final class PyDevCondaPreferences {

    private static final String CONDA_PATH = "CONDA_PATH";
    private static final String DEFAULT_CONDA_PATH = "";

    /**
     * @return the conda executable or null if it couldn't be found.
     */
    public static final File getExecutable() {
        String condaPath = PydevPrefs.getEclipsePreferences().get(CONDA_PATH, DEFAULT_CONDA_PATH);
        if (condaPath.isEmpty()) {
            return null;
        }
        File condaExecutable = new File(condaPath);
        if (!condaExecutable.exists()) {
            Log.log("Conda set in preferences: " + condaPath + " does not exist.");
            return null;
        }
        return condaExecutable;
    }

    public static final File setExecutable(File condaExecutable) {
        PydevPrefs.getEclipsePreferences().put(CONDA_PATH, condaExecutable.getPath());
        return condaExecutable;
    }

}
