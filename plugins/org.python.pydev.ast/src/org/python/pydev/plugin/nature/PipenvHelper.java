package org.python.pydev.plugin.nature;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.runners.SimpleExeRunner;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.PlatformUtils;

public class PipenvHelper {

    public static IInterpreterInfo getPipenvInterpreterInfoForProjectLocation(IInterpreterInfo[] interpretersInfo,
            File projectlocation, IInterpreterManager interpreterManager) {
        IInterpreterInfo info = PipenvHelper.getInterpreterInfoWithTargetDir(interpretersInfo, projectlocation);
        if (info != null) {
            return info;
        }
        if (projectlocation.exists()
                && new File(projectlocation, "Pipfile").exists()) {
            // the info was not marked as a pipenv project (but it's valid anyways).
            String pipenvLocation = PipenvHelper.searchDefaultPipenvLocation(interpretersInfo[0], interpreterManager);
            if (pipenvLocation != null) {
                File pythonVenvFromLocation = PipenvHelper
                        .getPythonExecutableFromProjectLocationWithPipenv(pipenvLocation, projectlocation);
                for (IInterpreterInfo tempInfo : interpretersInfo) {
                    if (new File(tempInfo.getExecutableOrJar()).equals(pythonVenvFromLocation)) {
                        return tempInfo;
                    }
                }
            }
        }
        return null;
    }

    public static IInterpreterInfo getInterpreterInfoWithTargetDir(IInterpreterInfo[] interpretersInfo,
            File pipenvTargetDir) {
        for (IInterpreterInfo info : interpretersInfo) {
            if (info.getPipenvTargetDir() != null
                    && new File(info.getPipenvTargetDir()).equals(pipenvTargetDir)) {
                return info;
            }
        }
        return null;
    }

    private static String computedPipEnvLocation = null;

    public static String searchDefaultPipenvLocation(IInterpreterInfo interpreterInfo,
            IInterpreterManager interpreterManager) {
        String ret = PydevPrefs.getEclipsePreferences().get("DEFAULT_PIPENV_LOCATION", "");
        if (!ret.trim().isEmpty()) {
            return ret;
        }
        if (interpreterInfo != null) {
            // if available, store computed location in the interpreter.
            String computed = interpreterInfo.getComputedPipEnvLocation();
            if (computed == null) {
                interpreterInfo.setComputedPipEnvLocation(computePipEnvLocation(interpreterInfo, interpreterManager)); // may still be null.
            }
            return interpreterInfo.getComputedPipEnvLocation(); // may still be null.
        } else {
            if (computedPipEnvLocation == null) {
                computedPipEnvLocation = computePipEnvLocation(interpreterInfo, interpreterManager); // may still be null
            }
            return computedPipEnvLocation;
        }
    }

    private static String computePipEnvLocation(IInterpreterInfo interpreterInfo,
            IInterpreterManager interpreterManager) {
        Set<String> pathsToSearch = PythonNature.getPathsToSearch();
        List<File> searchedDirectories = new ArrayList<>();
        for (String string : pathsToSearch) {
            File file = InterpreterInfo.searchExecutableInContainer("pipenv", new File(string),
                    searchedDirectories);
            if (file != null) {
                return file.getAbsolutePath();
            }
        }
        // If it still didn't find it, search in the base interpreter.
        if (interpreterInfo != null) {
            String executableOrJar = interpreterInfo.getExecutableOrJar();
            File file = InterpreterInfo.searchExecutableInContainer("pipenv", new File(executableOrJar).getParentFile(),
                    searchedDirectories);
            if (file != null) {
                return file.getAbsolutePath();
            }

            // Ok, not installed in the interpreter, let's search in the user site packages.
            String userInfo = interpreterInfo.obtainUserSitePackages(interpreterManager);
            if (!userInfo.isEmpty()) {
                File f = new File(userInfo);
                if (f.exists()) {
                    if (PlatformUtils.isWindowsPlatform()) {
                        // On windows we have to remove the site-packages (and later we'll search for
                        // this file + Scripts).
                        // On Linux we should just add `bin` to it, so, it should be ok.
                        f = f.getParentFile();
                    }

                    file = InterpreterInfo.searchExecutableInContainer("pipenv", f, searchedDirectories);
                    if (file != null) {
                        return file.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }

    public static void storeDefaultPipenvLocation(String pipenvLocation) {
        PydevPrefs.getEclipsePreferences().put("DEFAULT_PIPENV_LOCATION", pipenvLocation);
    }

    private static Object projectLocationToPipenvPythonLocationChacheLock = new Object();
    private static Map<File, Optional<File>> projectLocationToPipenvPythonLocationChache = new HashMap<>();

    /**
     * @param pipenvLocation the pipenv executable (full path to pipenv.exe).
     * @param projectlocation the location for the target project directory (i.e.: c:\\my\\project)
     * @return the python executable for the given project.
     */
    public static File getPythonExecutableFromProjectLocationWithPipenv(final String pipenvLocation,
            final File projectlocation) {
        synchronized (PipenvHelper.projectLocationToPipenvPythonLocationChacheLock) {
            Optional<File> pipenvPythonLocation = PipenvHelper.projectLocationToPipenvPythonLocationChache
                    .get(projectlocation);
            if (pipenvPythonLocation != null) {
                if (pipenvPythonLocation.isPresent()) {
                    return pipenvPythonLocation.get();
                } else {
                    return null;
                }
            }
        }

        SimpleExeRunner runner = new SimpleExeRunner();
        Tuple<Process, String> processInfo = runner.run(new String[] { pipenvLocation, "--venv" },
                projectlocation, null, null);
        Tuple<String, String> processOutput = SimpleExeRunner.getProcessOutput(processInfo.o1,
                processInfo.o2, null, "utf-8");
        if (processInfo.o1.exitValue() == 0) {
            File venvLocation = new File(processOutput.o1.trim());
            if (venvLocation.exists()) {
                List<File> searchedDirectories = new ArrayList<File>();
                File pythonExecutable = InterpreterInfo.searchExecutableInContainer("python", venvLocation,
                        searchedDirectories);
                if (pythonExecutable != null && FileUtils.enhancedIsFile(pythonExecutable)) {
                    synchronized (PipenvHelper.projectLocationToPipenvPythonLocationChacheLock) {
                        PipenvHelper.projectLocationToPipenvPythonLocationChache.put(projectlocation,
                                Optional.of(pythonExecutable));
                    }
                    return pythonExecutable;
                }
            }
        }
        // Cache that we didn't find it!
        synchronized (PipenvHelper.projectLocationToPipenvPythonLocationChacheLock) {
            PipenvHelper.projectLocationToPipenvPythonLocationChache.put(projectlocation, Optional.empty());
        }
        return null;

    }

}
