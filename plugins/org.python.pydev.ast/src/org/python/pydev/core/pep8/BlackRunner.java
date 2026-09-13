package org.python.pydev.core.pep8;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.ast.runners.UniversalRunner.PythonRunner;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.formatter.ExternalFormatterException;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.formatter.LineRanges;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;

public class BlackRunner implements IExternalFormatterRunner {

    public static final String TOOL_NAME = "black";

    /**
     * black formats line ranges with <code>--line-ranges=START-END</code> (1-based, inclusive on both
     * ends; black 23.11+). The option may be repeated: one per contiguous block of lines.
     */
    public static String[] lineRangesArgs(int[] zeroBasedLines) {
        List<int[]> ranges = LineRanges.toOneBasedInclusiveRanges(zeroBasedLines);
        String[] args = new String[ranges.size()];
        for (int i = 0; i < args.length; i++) {
            int[] range = ranges.get(i);
            args[i] = "--line-ranges=" + range[0] + "-" + range[1];
        }
        return args;
    }

    @Override
    public String format(String filepath, IPythonNature nature, IDocument doc, FormatStd std, File workingDir,
            int[] zeroBasedLines) throws ExternalFormatterException {
        return formatWithBlack(filepath, nature, doc, std, workingDir, zeroBasedLines);
    }

    /**
     * @param filepath note that it may be null
     * @param zeroBasedLines the lines to format or null to format the whole document
     * @return the formatted contents
     */
    public static String formatWithBlack(String filepath, IPythonNature nature, IDocument doc, FormatStd std,
            File workingDir, int[] zeroBasedLines) throws ExternalFormatterException {
        try {
            Process process;
            String[] parseArguments = ProcessUtils.parseArguments(std.blackParameters);
            String cmdarrayAsStr;

            String[] pathArgs = filepath != null && filepath.length() > 0
                    ? new String[] { "--stdin-filename", filepath }
                    : new String[0];
            String[] rangeArgs = zeroBasedLines != null ? lineRangesArgs(zeroBasedLines) : new String[0];

            String executableLocation = std.blackExecutableLocation;
            if (!std.searchBlackInInterpreter && executableLocation != null && !executableLocation.isEmpty()
                    && FileUtils.enhancedIsFile(new File(executableLocation))) {
                SimpleRunner simpleRunner = new SimpleRunner();
                String[] args = ArrayUtils.concatArrays(new String[] { executableLocation, "-" }, pathArgs,
                        rangeArgs, parseArguments);
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

                Tuple<Process, String> processInfo = pythonRunner.createProcessFromModuleName(TOOL_NAME,
                        ArrayUtils.concatArrays(new String[] { "-" }, pathArgs, rangeArgs, parseArguments),
                        workingDir, new NullProgressMonitor());
                process = processInfo.o1;
                cmdarrayAsStr = processInfo.o2;
            }

            String pythonFileEncoding = FileUtils.getPythonFileEncoding(doc, null);
            if (pythonFileEncoding == null) {
                pythonFileEncoding = "utf-8";
            }
            return RunnerCommon.writeContentsAndGetOutputOrThrow(doc.get().getBytes(pythonFileEncoding),
                    pythonFileEncoding, process, cmdarrayAsStr, TOOL_NAME);
        } catch (ExternalFormatterException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalFormatterException(TOOL_NAME, "Unable to run black: " + e.getMessage(), e);
        }
    }

}
