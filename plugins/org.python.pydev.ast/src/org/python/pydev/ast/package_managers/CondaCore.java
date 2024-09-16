package org.python.pydev.ast.package_managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.log.Log;
import org.python.pydev.json.eclipsesource.JsonArray;
import org.python.pydev.json.eclipsesource.JsonObject;
import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.OrderedSet;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.PlatformUtils;

public class CondaCore {

    public static List<File> listCondaEnvironments(File condaExecutable) {
        String encoding = "utf-8";
        Tuple<String, String> output = new SimpleRunner().runAndGetOutput(
                new String[] { condaExecutable.toString(), "env", "list", "--json" }, null, null,
                null,
                encoding);
        Log.logInfo(output.o1);
        if (output.o2 != null && output.o2.length() > 0) {
            Log.logInfo("STDERR when listing conda environments:\n" + output.o2);

        }
        JsonObject jsonOutput = JsonValue.readFrom(output.o1).asObject();
        JsonArray envs = jsonOutput.get("envs").asArray();
        Set<File> set = new HashSet<>();
        for (JsonValue env : envs.values()) {
            set.add(new File(env.asString()));
        }
        return new ArrayList<File>(set);
    }

    public static List<NameAndExecutable> getCondaEnvsAsNameAndExecutable(List<File> envs) {
        List<NameAndExecutable> ret = new ArrayList<NameAndExecutable>();
        if (PlatformUtils.isWindowsPlatform()) {
            for (File env : envs) {
                File exec = new File(env, "python.exe");
                if (FileUtils.enhancedIsFile(exec)) {
                    ret.add(new NameAndExecutable(env.getName(), exec.getPath()));
                } else {
                    Log.logInfo("Did not find: " + exec + " in conda environment.");
                }
            }
        } else {
            for (File env : envs) {
                File exec = new File(new File(env, "bin"), "python");
                if (FileUtils.enhancedIsFile(exec)) {
                    ret.add(new NameAndExecutable(env.getName(), exec.getPath()));
                } else {
                    Log.logInfo("Did not find: " + exec + " in conda environment.");
                }
            }
        }
        return ret;
    }

    /**
     * @return null if it couldn't be found, otherwise provides the conda
     * executable found in the system.
     */
    public static File findCondaExecutableInSystem() {
        File condaExecutable = null;
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
        return condaExecutable;
    }

}
