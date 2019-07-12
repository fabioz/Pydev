package org.python.pydev.core.pep8;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.UniversalRunner.PythonRunner;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;

public class BlackRunner {

    public static String formatWithBlack(IPythonNature nature, IDocument doc, String parameters, File workingDir) {
        if (nature == null) {
            IInterpreterManager manager = InterpreterManagersAPI.getPythonInterpreterManager();
            try {
                nature = new SystemPythonNature(manager);
            } catch (MisconfigurationException e) {
                Log.log(e);
            }
        }
        PythonRunner pythonRunner = new PythonRunner(nature);
        String[] parseArguments = ProcessUtils.parseArguments(parameters);

        Tuple<Process, String> processInfo = pythonRunner.createProcessFromModuleName("black",
                ArrayUtils.concatArrays(new String[] { "-" }, parseArguments),
                workingDir, new NullProgressMonitor());
        Process process = processInfo.o1;
        try {
            String pythonFileEncoding = FileUtils.getPythonFileEncoding(doc, null);
            if (pythonFileEncoding == null) {
                pythonFileEncoding = "utf-8";
            }
            process.getOutputStream().write(doc.get().getBytes(pythonFileEncoding));
            Tuple<String, String> processOutput = ProcessUtils.getProcessOutput(process, processInfo.o2,
                    new NullProgressMonitor(), pythonFileEncoding);
            if (process.exitValue() != 0) {
                Log.log("Black formatter exited with: " + process.exitValue() + "\nStdout:\n" + processOutput.o1
                        + "\nStderr:\n" + processOutput.o2);
                return null;
            }
            return processOutput.o1;
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

}
