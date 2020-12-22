package org.python.pydev.ui.pythonpathconf.conda;

import java.io.File;

import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.ui.pythonpathconf.package_manager.CondaPackageManager;

public final class PyDevCondaPreferences {
    private static final String CONDA_EXEC_PATH = "CONDA_EXEC_PATH";
    private static final String DEFAULT_CONDA_EXEC_PATH = "";

    public static final String getExecutablePath(CondaPackageManager condaPackageManager)
            throws UnableToFindExecutableException {
        String condaExecutable = PydevPrefs.getEclipsePreferences().get(CONDA_EXEC_PATH, DEFAULT_CONDA_EXEC_PATH);
        if (condaExecutable.isEmpty()) {
            return condaPackageManager.findCondaExecutable().toString();
        }
        return condaExecutable;
    }

    public static final void setStoredExecutablePath(String executableFullPath) {
        if (new File(executableFullPath).exists()) {
            PydevPrefs.getEclipsePreferences().put(CONDA_EXEC_PATH, executableFullPath);
        }
    }

}
