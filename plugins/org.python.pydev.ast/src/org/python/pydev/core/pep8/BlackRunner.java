package org.python.pydev.core.pep8;

import java.io.File;

import org.eclipse.core.resources.IFile;
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

    public static String formatWithBlack(IPythonNature nature, IFile file, IDocument doc, FormatStd std,
            File workingDir) {
        try {
            Process process;
            String[] parseArguments = ProcessUtils.parseArguments(std.blackParameters);
            String cmdarrayAsStr;

            String ext;
            if (file == null) {
                Log.log("BlackRunner: File not supplied");
            } else {
                Log.logInfo("BlackRunner: Running  for file: " + file);
            }
            boolean isPyi = file != null && (ext = file.getFileExtension()) != null & ext.equalsIgnoreCase("pyi");

            String executableLocation = std.blackExecutableLocation;

            //TEMP
            System.out.println(std);

            if (!std.searchBlackInInterpreter && executableLocation != null && !executableLocation.isEmpty()
                    && FileUtils.enhancedIsFile(new File(executableLocation))) {
                SimpleRunner simpleRunner = new SimpleRunner();
                String[] args = ArrayUtils.concatArrays(isPyi ? new String[] { executableLocation, "-", "--pyi" }
                        : new String[] { executableLocation, "-" }, parseArguments);
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
                String[] args = ArrayUtils.concatArrays(
                        isPyi ? new String[] { "-", "--pyi" } : new String[] { "-" },
                        parseArguments);
                Tuple<Process, String> processInfo = pythonRunner.createProcessFromModuleName("black",
                        args,
                        workingDir, new NullProgressMonitor());
                process = processInfo.o1;
                cmdarrayAsStr = processInfo.o2;
            }

            String pythonFileEncoding = FileUtils.getPythonFileEncoding(doc, null);
            if (pythonFileEncoding == null) {
                pythonFileEncoding = "utf-8";
            }
            boolean failedWrite = false;
            try {
                process.getOutputStream().write(doc.get().getBytes(pythonFileEncoding));
            } catch (Exception e) {
                failedWrite = true;
            }
            Tuple<String, String> processOutput = ProcessUtils.getProcessOutput(process, cmdarrayAsStr,
                    new NullProgressMonitor(), pythonFileEncoding);

            if (process.exitValue() != 0 || failedWrite) {
                Log.log("Black formatter exited with: " + process.exitValue() + " failedWrite: " + failedWrite
                        + "\nStdout:\n" + processOutput.o1
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
