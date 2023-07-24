package org.python.pydev.core.pep8;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.ast.runners.UniversalRunner.PythonRunner;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;

public class BlackRunner {

    /**
     * @param filepath note that it may be null
     * @return
     */
    public static String formatWithBlack(String filepath, IPythonNature nature, IDocument doc, FormatStd std,
            File workingDir) {
        try {
            Process process;
            String[] parseArguments = ProcessUtils.parseArguments(std.blackParameters);
            String cmdarrayAsStr;

            String executableLocation = std.blackExecutableLocation;
            if (!std.searchBlackInInterpreter && executableLocation != null && !executableLocation.isEmpty()
                    && FileUtils.enhancedIsFile(new File(executableLocation))) {
                SimpleRunner simpleRunner = new SimpleRunner();
                String[] args = ArrayUtils.concatArrays(new String[] { executableLocation, "-" }, parseArguments);
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

                String[] pathArgs = filepath != null && filepath.length() > 0
                        ? new String[] { "--stdin-filename", filepath }
                        : new String[0];

                Tuple<Process, String> processInfo = pythonRunner.createProcessFromModuleName("black",
                        ArrayUtils.concatArrays(new String[] { "-" }, pathArgs, parseArguments),
                        workingDir, new NullProgressMonitor());
                process = processInfo.o1;
                cmdarrayAsStr = processInfo.o2;
            }

            String pythonFileEncoding = FileUtils.getPythonFileEncoding(doc, null);
            if (pythonFileEncoding == null) {
                pythonFileEncoding = "utf-8";
            }
            return RunnerCommon.writeContentsAndGetOutput(doc.get().getBytes(pythonFileEncoding), pythonFileEncoding,
                    process, cmdarrayAsStr, "black");
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

}
