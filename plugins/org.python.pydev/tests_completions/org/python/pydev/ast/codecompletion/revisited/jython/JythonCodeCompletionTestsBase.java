/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 13/08/2005
 */
package org.python.pydev.ast.codecompletion.revisited.jython;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.ast.interpreter_managers.AbstractInterpreterManager;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.interpreter_managers.JythonInterpreterManager;
import org.python.pydev.ast.listing_utils.JavaVmLocationFinder;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback0;

public class JythonCodeCompletionTestsBase extends CodeCompletionTestsBase {

    protected boolean calledJavaExecutable = false;
    protected boolean calledJavaJars = false;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!new File(TestDependent.JAVA_RT_JAR_LOCATION).exists()) {
            throw new AssertionError(TestDependent.JAVA_RT_JAR_LOCATION + " does not exist.");
        }
        if (!new File(TestDependent.JYTHON_LIB_LOCATION).exists()) {
            throw new AssertionError(TestDependent.JYTHON_LIB_LOCATION + " does not exist.");
        }
        if (!new File(TestDependent.JAVA_LOCATION).exists()) {
            throw new AssertionError(TestDependent.JAVA_LOCATION + " does not exist.");
        }

        //we also need to set from where the info on the java env
        JavaVmLocationFinder.callbackJavaExecutable = new ICallback0<File>() {
            @Override
            public File call() {
                calledJavaExecutable = true;
                return new File(TestDependent.JAVA_LOCATION);
            }
        };

        //and on the associated jars to the java runtime
        JavaVmLocationFinder.callbackJavaJars = new ICallback0<List<File>>() {
            @Override
            public List<File> call() {
                calledJavaJars = true;
                ArrayList<File> jars = new ArrayList<File>();
                jars.add(new File(TestDependent.JAVA_RT_JAR_LOCATION));
                return jars;
            }
        };
    }

    @Override
    protected void afterRestorSystemPythonPath(InterpreterInfo info) {
        super.afterRestorSystemPythonPath(info);
        assertTrue(calledJavaExecutable);
        assertTrue(calledJavaJars);

        boolean foundRtJar = false;
        for (Object lib : info.libs) {
            String s = (String) lib;
            if (s.endsWith("rt.jar")) {
                foundRtJar = true;
            }
        }
        assertTrue(foundRtJar);
    }

    @Override
    protected PythonNature createNature() {
        return new PythonNature() {
            @Override
            public int getInterpreterType() throws CoreException {
                return IInterpreterManager.INTERPRETER_TYPE_JYTHON;
            }

            @Override
            public int getGrammarVersion() {
                return IPythonNature.GRAMMAR_PYTHON_VERSION_3_5;
            }
        };
    }

    @Override
    protected IInterpreterManager getInterpreterManager() {
        return InterpreterManagersAPI.getJythonInterpreterManager();
    }

    @Override
    protected void setInterpreterManager(String path) {
        AbstractInterpreterManager interpreterManager = new JythonInterpreterManager(this.getPreferences());

        InterpreterInfo info;
        info = (InterpreterInfo) interpreterManager.createInterpreterInfo(TestDependent.JYTHON_JAR_LOCATION,
                new NullProgressMonitor(), false);
        if (!info.executableOrJar.equals(TestDependent.JYTHON_JAR_LOCATION)) {
            throw new RuntimeException("expected same");
        }
        if (path != null) {
            info = new InterpreterInfo(info.getVersion(), TestDependent.JYTHON_JAR_LOCATION,
                    PythonPathHelper.parsePythonPathFromStr(path, new ArrayList<String>()));
        }

        interpreterManager.setInfos(new IInterpreterInfo[] { info }, null, null);
        InterpreterManagersAPI.setJythonInterpreterManager(interpreterManager);

    }

    /**
     * @see #restorePythonPath(boolean)
     *
     * same as the restorePythonPath function but also includes the site packages in the distribution
     */
    @Override
    public void restorePythonPathWithSitePackages(boolean force) {
        throw new RuntimeException("not available for jython");
    }

    /**
     * restores the pythonpath with the source library (system manager) and the source location for the tests (project manager)
     *
     * @param force whether this should be forced, even if it was previously created for this class
     */
    @Override
    public void restorePythonPath(boolean force) {
        restoreSystemPythonPath(force, TestDependent.JYTHON_LIB_LOCATION + "|" + TestDependent.JAVA_RT_JAR_LOCATION);
        restoreProjectPythonPath(force, TestDependent.TEST_PYSRC_TESTING_LOC);
        restoreProjectPythonPath2(force, TestDependent.TEST_PYSRC_TESTING_LOC2);
        checkSize();
    }

}
