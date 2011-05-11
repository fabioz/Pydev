/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
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
import org.python.pydev.runners.SimpleIronpythonRunner;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class IronpythonInterpreterManager extends AbstractInterpreterManager{

    public IronpythonInterpreterManager(IPreferenceStore prefs) {
        super(prefs);
    }

    @Override
    protected String getPreferenceName() {
        return IRONPYTHON_INTERPRETER_PATH;
    }
    
    @Override
    protected String getNotConfiguredInterpreterMsg() {
        return "Interpreter is not properly configured!\n" +
                "Please go to window > preferences > PyDev > Iron Python Interpreters and configure it.\n" +
                "If this is not supposed to be an Iron Python project, change the project type on the\n" +
                "project properties to the project you want (e.g.: Python project).";
    }

    @Override
    public Tuple<InterpreterInfo,String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor, boolean askUser) throws CoreException {
        return doCreateInterpreterInfo(executable, monitor, askUser);
    }

    /**
     * @param executable the iron python interpreter from where we should create the info
     * @param monitor a monitor to see the progress
     * 
     * @return the created interpreter info
     * @throws CoreException
     */
    public static Tuple<InterpreterInfo,String> doCreateInterpreterInfo(String executable, IProgressMonitor monitor, boolean askUser) throws CoreException {
        boolean isJythonExecutable = InterpreterInfo.isJythonExecutable(executable);
        if(isJythonExecutable){
            throw new RuntimeException("A jar cannot be used in order to get the info for the iron python interpreter.");
        }                

        File script = PydevPlugin.getScriptWithinPySrc("interpreterInfo.py");

        Tuple<String, String> outTup = new SimpleIronpythonRunner().runAndGetOutputWithInterpreter(
                executable, REF.getFileAbsolutePath(script), null, null, null, monitor);
        InterpreterInfo info = createInfoFromOutput(monitor, outTup, askUser);
        
        if(info == null){
            //cancelled
            return null;
        }

        info.restoreCompiledLibs(monitor);
        
        return new Tuple<InterpreterInfo,String>(info, outTup.o1);
    }

    public int getInterpreterType() {
        return IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON;
    }
    

    public String getManagerRelatedName() {
        return "ironpython";
    }

}
