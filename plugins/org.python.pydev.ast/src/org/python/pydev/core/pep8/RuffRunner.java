package org.python.pydev.core.pep8;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
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
import org.python.pydev.shared_core.io.PyUnsupportedEncodingException;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;

public class RuffRunner implements IExternalFormatterRunner {

    public static final String TOOL_NAME = "ruff";

    /**
     * A single run of <code>ruff format</code> over some contents.
     */
    public interface IRuffInvocation {

        /**
         * @param rangeArgOrNull the <code>--range=...</code> argument or null to format everything.
         * @return the formatted contents.
         */
        String run(String contents, String rangeArgOrNull) throws ExternalFormatterException;
    }

    /**
     * ruff formats a range with <code>--range=START-END</code> (ruff 0.2.1+): positions are 1-based
     * and END is exclusive, so a block of whole lines ends at the start of the line after it. When
     * the block reaches the last line the end is left out, which means "up to the end of the file".
     *
     * @param oneBasedInclusiveRange {firstLine, lastLine}, 1-based, inclusive.
     * @param numberOfLines the number of lines in the document being formatted.
     */
    public static String rangeArg(int[] oneBasedInclusiveRange, int numberOfLines) {
        int first = oneBasedInclusiveRange[0];
        int last = oneBasedInclusiveRange[1];
        if (last >= numberOfLines) {
            return "--range=" + first;
        }
        return "--range=" + first + "-" + (last + 1);
    }

    /**
     * ruff accepts a single <code>--range</code> per run, so each contiguous block of lines is formatted
     * in its own run. Blocks are processed from the bottom of the document upwards: a run may add or
     * remove lines, and going upwards keeps the line numbers of the blocks still to be formatted valid.
     *
     * @param zeroBasedLines the lines to format or null to format the whole contents in a single run.
     */
    public static String formatWithInvocation(IRuffInvocation invocation, String contents, int[] zeroBasedLines)
            throws ExternalFormatterException {
        if (zeroBasedLines == null) {
            return invocation.run(contents, null);
        }
        List<int[]> ranges = LineRanges.toOneBasedInclusiveRanges(zeroBasedLines);
        int numberOfLines = new Document(contents).getNumberOfLines();
        String current = contents;
        for (int i = ranges.size() - 1; i >= 0; i--) {
            current = invocation.run(current, rangeArg(ranges.get(i), numberOfLines));
        }
        return current;
    }

    @Override
    public String format(String filepath, IPythonNature nature, IDocument doc, FormatStd std, File workingDir,
            int[] zeroBasedLines) throws ExternalFormatterException {
        String encoding;
        try {
            String pythonFileEncoding = FileUtils.getPythonFileEncoding(doc, null);
            encoding = pythonFileEncoding != null ? pythonFileEncoding : "utf-8";
        } catch (PyUnsupportedEncodingException e) {
            throw new ExternalFormatterException(TOOL_NAME, "Unable to determine the file encoding: " + e.getMessage(),
                    e);
        }
        IRuffInvocation invocation = (contents, rangeArgOrNull) -> runRuff(filepath, nature, std, workingDir,
                encoding, contents, rangeArgOrNull);
        return formatWithInvocation(invocation, doc.get(), zeroBasedLines);
    }

    /**
     * @param filepath note that it may be null
     * @param zeroBasedLines the lines to format or null to format the whole document
     * @return the formatted contents
     */
    public static String formatWithRuff(String filepath, IPythonNature nature, IDocument doc, FormatStd std,
            File workingDir, int[] zeroBasedLines) throws ExternalFormatterException {
        return new RuffRunner().format(filepath, nature, doc, std, workingDir, zeroBasedLines);
    }

    private static String runRuff(String filepath, IPythonNature nature, FormatStd std, File workingDir,
            String encoding, String contents, String rangeArgOrNull) throws ExternalFormatterException {
        try {
            Process process;
            String[] parseArguments = ProcessUtils.parseArguments(std.ruffParameters);
            String cmdarrayAsStr;

            String[] pathArgs = filepath != null && filepath.length() > 0
                    ? new String[] { "--stdin-filename", filepath }
                    : new String[0];
            String[] rangeArgs = rangeArgOrNull != null ? new String[] { rangeArgOrNull } : new String[0];

            String executableLocation = std.ruffExecutableLocation;
            if (!std.searchRuffInInterpreter && executableLocation != null && !executableLocation.isEmpty()
                    && FileUtils.enhancedIsFile(new File(executableLocation))) {
                SimpleRunner simpleRunner = new SimpleRunner();
                String[] args = ArrayUtils.concatArrays(new String[] { executableLocation, "format", "-" },
                        pathArgs, rangeArgs, parseArguments);
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
                        ArrayUtils.concatArrays(new String[] { "format", "-" }, pathArgs, rangeArgs,
                                parseArguments),
                        workingDir, new NullProgressMonitor());
                process = processInfo.o1;
                cmdarrayAsStr = processInfo.o2;
            }

            return RunnerCommon.writeContentsAndGetOutputOrThrow(contents.getBytes(encoding), encoding, process,
                    cmdarrayAsStr, TOOL_NAME);
        } catch (ExternalFormatterException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalFormatterException(TOOL_NAME, "Unable to run ruff: " + e.getMessage(), e);
        }
    }

}
