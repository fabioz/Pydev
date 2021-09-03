package org.python.pydev.ast.interpreter_managers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.OrderedSet;
import org.python.pydev.shared_core.utils.PlatformUtils;

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
            // Unable to find, let's see if it's in the path
            OrderedSet<String> pathsToSearch = new OrderedSet<>(PythonNature.getPathsToSearch());
            // use ordered set: we want to search the PATH before hard-coded paths.
            String userHomeDir = System.getProperty("user.home");
            if (PlatformUtils.isWindowsPlatform()) {
                pathsToSearch.add("c:/tools/miniconda");
                pathsToSearch.add("c:/tools/miniconda2");
                pathsToSearch.add("c:/tools/miniconda3");
                pathsToSearch.add("c:/tools/conda");
                pathsToSearch.add("c:/tools/conda2");
                pathsToSearch.add("c:/tools/conda3");
            } else {
                pathsToSearch.add("/opt/conda");
                pathsToSearch.add("/opt/conda/bin");
                pathsToSearch.add("/usr/bin");
            }
            pathsToSearch.add(new File(userHomeDir, "miniconda").toString());
            pathsToSearch.add(new File(userHomeDir, "miniconda2").toString());
            pathsToSearch.add(new File(userHomeDir, "miniconda3").toString());
            pathsToSearch.add(new File(userHomeDir, "conda").toString());
            pathsToSearch.add(new File(userHomeDir, "conda2").toString());
            pathsToSearch.add(new File(userHomeDir, "conda3").toString());
            pathsToSearch.add(new File(userHomeDir, "Anaconda").toString());
            pathsToSearch.add(new File(userHomeDir, "Anaconda2").toString());
            pathsToSearch.add(new File(userHomeDir, "Anaconda3").toString());
            pathsToSearch.add(new File(userHomeDir).toString());

            List<File> searchedDirectories = new ArrayList<>();
            for (String string : pathsToSearch) {
                File file = InterpreterInfo.searchExecutableInContainer("conda", new File(string),
                        searchedDirectories);
                if (file != null) {
                    condaExecutable = file;
                }
            }
            if (condaExecutable == null) {
                throw e;
            }
        }
        return condaExecutable;
    }

}
