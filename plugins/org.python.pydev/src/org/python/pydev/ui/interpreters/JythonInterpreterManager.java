/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.copiedfromeclipsesrc.JavaVmLocationFinder;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class JythonInterpreterManager extends AbstractInterpreterManager {

    public JythonInterpreterManager(IPreferenceStore preferences) {
        super(preferences);
    }

    @Override
    protected String getPreferenceName() {
        return JYTHON_INTERPRETER_PATH;
    }

    @Override
    public String getInterpreterUIName() {
        return "Jython";
    }

    @Override
    public Tuple<InterpreterInfo, String> internalCreateInterpreterInfo(String executable, IProgressMonitor monitor,
            boolean askUser) throws CoreException, JDTNotAvailableException {
        return doCreateInterpreterInfo(executable, monitor, askUser);
    }

    @Override
    protected String getPreferencesPageId() {
        return "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPageJython";
    }

    /**
     * This is the method that creates the interpreter info for jython. It gets the info on the jython side and on the java side
     * 
     * @param executable the jar that should be used to get the info
     * @param monitor a monitor, to keep track of what's happening
     * @return the interpreter info, with the default libraries and jars
     * 
     * @throws CoreException
     */
    public static Tuple<InterpreterInfo, String> doCreateInterpreterInfo(String executable, IProgressMonitor monitor,
            boolean askUser) throws CoreException, JDTNotAvailableException {
        boolean isJythonExecutable = InterpreterInfo.isJythonExecutable(executable);

        if (!isJythonExecutable) {
            throw new RuntimeException(
                    "In order to get the info for the jython interpreter, a jar is needed (e.g.: jython.jar)");
        }
        File script = getInterpreterInfoPy();

        //gets the info for the python side
        Tuple<String, String> outTup = new SimpleJythonRunner().runAndGetOutputWithJar(
                FileUtils.getFileAbsolutePath(script),
                executable, null, null, null, monitor, "utf-8");

        String output = outTup.o1;

        InterpreterInfo info = createInfoFromOutput(monitor, outTup, askUser, executable, false);
        if (info == null) {
            //cancelled
            return null;
        }
        //the executable is the jar itself
        info.executableOrJar = executable;

        //we have to find the jars before we restore the compiled libs 
        List<File> jars = JavaVmLocationFinder.findDefaultJavaJars();
        for (File jar : jars) {
            info.libs.add(FileUtils.getFileAbsolutePath(jar));
        }

        //java, java.lang, etc should be found now
        info.restoreCompiledLibs(monitor);

        return new Tuple<InterpreterInfo, String>(info, output);
    }

    public int getInterpreterType() {
        return IInterpreterManager.INTERPRETER_TYPE_JYTHON;
    }

    public String getManagerRelatedName() {
        return "jython";
    }
}
