/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Aug 16, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.shell;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.SimplePythonRunner;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.io.FileUtils;

/**
 * @author Fabio Zadrozny
 */
public class PythonShell extends CompletionsShell {

    /**
     * Initialize with the default python server file.
     *
     * @throws IOException
     * @throws CoreException
     */
    public PythonShell() throws IOException, CoreException {
        super(CorePlugin.getScriptWithinPySrc("pycompletionserver.py"));
    }

    @Override
    protected synchronized ProcessCreationInfo createServerProcess(IInterpreterInfo interpreter, int port)
            throws IOException {
        File file = new File(interpreter.getExecutableOrJar());
        if (file.isDirectory()) {
            throw new RuntimeException("The interpreter location found is a directory. " + interpreter);
        }

        String[] parameters = SimplePythonRunner.preparePythonCallParameters(interpreter.getExecutableOrJar(),
                FileUtils.getFileAbsolutePath(serverFile), new String[] { "" + port });

        IInterpreterManager manager = InterpreterManagersAPI.getPythonInterpreterManager();

        String[] envp = null;
        try {
            envp = SimpleRunner.getEnvironment(null, interpreter, manager);
        } catch (CoreException e) {
            Log.log(e);
        }

        File workingDir = serverFile.getParentFile();

        return new ProcessCreationInfo(parameters, envp, workingDir, SimpleRunner.createProcess(parameters, envp,
                workingDir));
    }

}