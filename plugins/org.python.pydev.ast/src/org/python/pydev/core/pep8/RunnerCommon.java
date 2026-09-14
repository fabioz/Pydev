package org.python.pydev.core.pep8;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.formatter.ExternalFormatterException;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class RunnerCommon {

    /**
     * How long an external tool may take before it's killed.
     */
    public static final int TIMEOUT_SECONDS = 60;

    /**
     * Writes the contents to the process stdin, waits for it to finish and returns its stdout.
     *
     * @throws ExternalFormatterException if the process could not be run, exited with a non-zero code
     * or did not finish within {@link #TIMEOUT_SECONDS} (in which case it's killed).
     */
    public static String writeContentsAndGetOutputOrThrow(byte[] fileContents, String encoding, Process process,
            String cmdarrayAsStr, String toolName) throws ExternalFormatterException {
        CompletableFuture<Tuple<String, String>> future = new CompletableFuture<>();

        // Isort will start to write as we're writing the input, so, we need to start
        // reading before we write (it can't be sequential otherwise it can deadlock).

        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    boolean closeOutput = false;
                    future.complete(ProcessUtils.getProcessOutput(process, cmdarrayAsStr,
                            new NullProgressMonitor(), encoding, closeOutput));
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }
        };
        t.start();

        boolean failedWrite = false;
        try {
            process.getOutputStream().write(fileContents);
            process.getOutputStream().flush();
            process.getOutputStream().close();
        } catch (Exception e) {
            failedWrite = true;
        }

        Tuple<String, String> processOutput;
        try {
            processOutput = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            process.destroyForcibly();
            throw new ExternalFormatterException(toolName, toolName + " did not finish within " + TIMEOUT_SECONDS
                    + " seconds and was killed.\nCommand: " + cmdarrayAsStr, e);
        } catch (InterruptedException | ExecutionException e) {
            throw new ExternalFormatterException(toolName,
                    toolName + " could not be run: " + e.getMessage() + "\nCommand: " + cmdarrayAsStr, e);
        }

        int exitValue = process.exitValue();
        if (exitValue != 0 || failedWrite) {
            throw new ExternalFormatterException(toolName, toolName + " exited with: " + exitValue
                    + (failedWrite ? " (writing its input failed)" : "")
                    + "\nCommand: " + cmdarrayAsStr
                    + "\nStdout:\n" + processOutput.o1
                    + "\nStderr:\n" + processOutput.o2);
        }
        return processOutput.o1;
    }

    /**
     * Same as {@link #writeContentsAndGetOutputOrThrow}, but failures are only logged and null is returned.
     */
    public static String writeContentsAndGetOutput(byte[] fileContents, String encoding, Process process,
            String cmdarrayAsStr, String toolName) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return writeContentsAndGetOutputOrThrow(fileContents, encoding, process, cmdarrayAsStr, toolName);
        } catch (ExternalFormatterException e) {
            Log.log(e);
            return null;
        }
    }

}
