package org.python.pydev.ast.interpreter_managers;

import java.io.File;

import org.python.pydev.ast.package_managers.CondaCore;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.io.FileUtils;

public final class PyDevCondaPreferences {

    public static final String CONDA_PATH = "CONDA_PATH";
    public static final String DEFAULT_CONDA_PATH = "";

    /**
     * @return the conda executable or null if it couldn't be found.
     */
    public static final File getExecutable() {
        String condaPath = PydevPrefs.getEclipsePreferences().get(CONDA_PATH, DEFAULT_CONDA_PATH);
        if (condaPath.isEmpty()) {
            return null;
        }
        File condaExecutable = new File(condaPath);
        if (!FileUtils.enhancedIsFile(condaExecutable)) {
            Log.log("Conda set in preferences: " + condaPath + " does not exist.");
            return null;
        }
        return condaExecutable;
    }

    public static final File setExecutable(File condaExecutable) {
        PydevPrefs.getEclipsePreferences().put(CONDA_PATH, condaExecutable.getPath());
        return condaExecutable;
    }

    public static File findCondaExecutable(IInterpreterInfo interpreterInfo) throws UnableToFindExecutableException {

        // Try to get from the preferences first.
        File condaExecutable = PyDevCondaPreferences.getExecutable();
        if (condaExecutable != null) {
            return condaExecutable;
        }
        try {
            condaExecutable = interpreterInfo.searchExecutableForInterpreter("conda", true);
        } catch (UnableToFindExecutableException e) {
            condaExecutable = CondaCore.findCondaExecutableInSystem();
            if (condaExecutable == null) {
                throw e;
            }
        }
        return condaExecutable;
    }

}
