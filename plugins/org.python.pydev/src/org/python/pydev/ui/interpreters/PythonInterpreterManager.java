/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.interpreters;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class PythonInterpreterManager extends AbstractInterpreterManager{

    public PythonInterpreterManager(IPreferenceStore preferences) {
        super(preferences);
    }

    @Override
    protected String getPreferenceName() {
        return PYTHON_INTERPRETER_PATH;
    }
    
    @Override
    protected String getNotConfiguredInterpreterMsg() {
        return "Interpreter is not properly configured!\n" +
                "Please go to window > preferences > PyDev > Python Interpreters and configure it.\n" +
                "If this is not supposed to be a Python project, change the project type on the\n" +
                "project properties to the project you want (e.g.: Jython project).";
    }

    @Override
    public Tuple<InterpreterInfo,String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException {
        return doCreateInterpreterInfo(executable, monitor);
    }

    /**
     * @param executable the python interpreter from where we should create the info
     * @param monitor a monitor to see the progress
     * 
     * @return the created interpreter info
     * @throws CoreException
     */
    public static Tuple<InterpreterInfo,String> doCreateInterpreterInfo(String executable, IProgressMonitor monitor) throws CoreException {
        boolean isJythonExecutable = InterpreterInfo.isJythonExecutable(executable);
        if(isJythonExecutable){
            throw new RuntimeException("A jar cannot be used in order to get the info for the python interpreter.");
        }                

        File script = PydevPlugin.getScriptWithinPySrc("interpreterInfo.py");

        Tuple<String, String> outTup = new SimplePythonRunner().runAndGetOutputWithInterpreter(executable, REF.getFileAbsolutePath(script), null, null, null, monitor);
        InterpreterInfo info = createInfoFromOutput(monitor, outTup);
        
        if(info == null){
            //cancelled
            return null;
        }

        info.restoreCompiledLibs(monitor);
        
        return new Tuple<InterpreterInfo,String>(info, outTup.o1);
    }

    public int getInterpreterType() {
        return IInterpreterManager.INTERPRETER_TYPE_PYTHON;
    }
    

    public String getManagerRelatedName() {
        return "python";
    }

}
