package org.python.pydev.ui.pythonpathconf.conda;

import java.io.File;

import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.preferences.PydevPrefs;

public final class PyDevCondaPreferences {
    private static final String CONDA_PATH = "CONDA_EXEC_PATH";
    private static final String DEFAULT_CONDA_PATH = "";

    public static final File getExecutable()
            throws UnableToFindExecutableException {
        String condaPath = PydevPrefs.getEclipsePreferences().get(CONDA_PATH, DEFAULT_CONDA_PATH);
        if (condaPath.isEmpty()) {
            throw new UnableToFindExecutableException("Conda executable not defined.");
        }
        File condaExecutable = new File(condaPath);
        if (!condaExecutable.exists()) {
            throw new UnableToFindExecutableException("Conda " + condaPath + " executable not found.");
        }
        return condaExecutable;
    }

    public static final File setExecutable(File condaExecutable) {
        PydevPrefs.getEclipsePreferences().put(CONDA_PATH, condaExecutable.getPath());
        return condaExecutable;
    }

}
