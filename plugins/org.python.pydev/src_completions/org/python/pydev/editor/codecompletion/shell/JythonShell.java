/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.shell;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.io.FileUtils;

public class JythonShell extends AbstractShell {

    public JythonShell() throws IOException, CoreException {
        super(PydevPlugin.getScriptWithinPySrc("pycompletionserver.py"));
    }

    /**
     * Will create the jython shell and return a string to be shown to the user with the jython shell command line.
     * @throws MisconfigurationException 
     */
    @Override
    protected synchronized ProcessCreationInfo createServerProcess(IInterpreterInfo jythonJar, int port)
            throws IOException, JDTNotAvailableException, MisconfigurationException {
        String script = FileUtils.getFileAbsolutePath(serverFile);
        String[] executableStr = SimpleJythonRunner.makeExecutableCommandStr(jythonJar.getExecutableOrJar(), script,
                "", String.valueOf(port));

        IInterpreterManager manager = PydevPlugin.getJythonInterpreterManager();

        String[] envp = null;
        try {
            envp = SimpleRunner.getEnvironment(null, jythonJar, manager);
        } catch (CoreException e) {
            Log.log(e);
        }

        File workingDir = serverFile.getParentFile();

        return new ProcessCreationInfo(executableStr, envp, workingDir, SimpleRunner.createProcess(executableStr, envp,
                workingDir));
    }

}
