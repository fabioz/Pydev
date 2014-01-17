/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.string.StringUtils;

public class ProcessCreationInfo {

    public final String[] parameters;
    public final String[] envp;
    public final File workingDir;
    private final Process process;

    private ThreadStreamReader stdReader;
    private ThreadStreamReader errReader;

    public ProcessCreationInfo(String[] parameters, String[] envp, File workingDir, Process process) {
        this.parameters = parameters;
        if (envp != null) {
            Arrays.sort(envp); //Keep it sorted!
        }
        this.envp = envp;
        this.workingDir = workingDir;
        this.process = process;

        try {
            process.getOutputStream().close(); //we won't write to it...
        } catch (IOException e2) {
        }

        //will print things if we are debugging or just get it (and do nothing except emptying it)
        stdReader = new ThreadStreamReader(process.getInputStream());
        errReader = new ThreadStreamReader(process.getErrorStream());

        stdReader.setName("Shell reader (stdout)");
        errReader.setName("Shell reader (stderr)");

        stdReader.start();
        errReader.start();
    }

    public String getProcessLog() {

        String joinedParams = StringUtils.join(" ", parameters);

        String environment = "EMPTY ENVIRONMENT";
        if (envp != null) {
            environment = StringUtils.join("\n", envp);
        }

        String workDir = "NULL WORK DIR";
        if (workingDir != null) {
            workDir = workingDir.toString();
        }

        String osName = System.getProperty("os.name");
        if (osName == null) {
            osName = "Unknown OS!";
        }

        String stdContents = stdReader.getContents();
        String errContents = errReader.getContents();

        //Pre-allocate it in a proper size.
        String[] splitted = new String[] { "ProcessInfo:\n\n - Executed: ", joinedParams, "\n\n - Environment:\n",
                environment, "\n\n - Working Dir:\n", workDir, "\n\n - OS:\n", osName, "\n\n - Std output:\n",
                stdContents, "\n\n - Err output:\n", errContents };

        return StringUtils.join("", splitted);
    }

    public String getAndClearContents() {
        String stdContents = stdReader.getContents();
        String errContents = errReader.getContents();

        return StringUtils.join("", "STDOUT: ", stdContents, "\nESTDERR: ", errContents, "\n");
    }

    private void stopGettingOutput() {
        if (stdReader != null) {
            stdReader.stopGettingOutput();
        }
        stdReader = null;
        if (errReader != null) {
            errReader.stopGettingOutput();
        }
        errReader = null;
    }

    public void destroy() {
        stopGettingOutput();
        try {
            this.process.destroy();
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public int exitValue() {
        return this.process.exitValue();
    }

    /**
     * Use to clear contents so that we don't become a heap sink!
     */
    public void clearOutput() {
        if (this.stdReader != null) {
            this.stdReader.clearContents();
        }
        if (this.errReader != null) {
            this.errReader.clearContents();
        }
    }

}