package org.python.pydev.core.pep8;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.ast.runners.UniversalRunner.PythonRunner;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;

public class ISortRunner {

    public static String formatWithISort(File targetFile, IPythonNature nature, String fileContents, String encoding,
            File workingDir, String[] isortArguments, Set<String> knownThirdParty,
            Optional<String> executableLocation) {
        try {
            Process process;
            String cmdarrayAsStr;

            if (executableLocation.isPresent() && !executableLocation.get().isEmpty()
                    && FileUtils.enhancedIsFile(new File(executableLocation.get()))) {
                SimpleRunner simpleRunner = new SimpleRunner();
                String[] args = ArrayUtils.concatArrays(new String[] { executableLocation.get(), "-" }, isortArguments);
                Tuple<Process, String> r = simpleRunner.run(args, workingDir, null, null);
                process = r.o1;
                cmdarrayAsStr = r.o2;
            } else {
                if (nature == null) {
                    IInterpreterManager manager = InterpreterManagersAPI.getPythonInterpreterManager();
                    try {
                        nature = new SystemPythonNature(manager);
                    } catch (MisconfigurationException e) {
                        Log.log(e);
                    }
                }
                PythonRunner pythonRunner = new PythonRunner(nature);
                String filepath = targetFile != null ? FileUtils.getFileAbsolutePath(targetFile)
                        : null;

                String[] pathArgs = filepath != null && filepath.length() > 0
                        ? new String[] { "--filename", filepath }
                        : new String[0];

                List<String> known = new ArrayList<>();
                // This is no longer done (let isort itself manage it now).
                // The issue in using it is that if you're developing a program with a namespace package,
                // it's possible that it'll have one part in the env and another as the actual sources.
                // for (String s : knownThirdParty) {
                //     known.add("-o");
                //     known.add(s);
                // }
                final String[] args = ArrayUtils.concatArrays(pathArgs, isortArguments, known.toArray(new String[0]),
                        new String[] { "-d", "-" });
                Tuple<Process, String> processInfo = pythonRunner.createProcessFromModuleName("isort",
                        args, workingDir, new NullProgressMonitor());
                process = processInfo.o1;
                cmdarrayAsStr = processInfo.o2;
            }

            return RunnerCommon.writeContentsAndGetOutput(fileContents.getBytes(encoding), encoding, process,
                    cmdarrayAsStr,
                    "isort");
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

}
