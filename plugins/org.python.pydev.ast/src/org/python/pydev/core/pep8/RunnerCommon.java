package org.python.pydev.core.pep8;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class RunnerCommon {

    public static String writeContentsAndGetOutput(byte[] fileContents, String encoding, Process process,
            String cmdarrayAsStr, String toolName) throws InterruptedException, ExecutionException, TimeoutException {
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
    
        // Wait a full minute before bailing out.
        try {
            Tuple<String, String> processOutput = future.get(60, TimeUnit.SECONDS);
    
            if (process.exitValue() != 0 || failedWrite) {
                Log.log(toolName + " exited with: " + process.exitValue() + " failedWrite: " + failedWrite
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
