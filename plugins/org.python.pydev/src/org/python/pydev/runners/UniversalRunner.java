/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.runners;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This class provides a factory for creating a suitable runner for any project and running
 * some code or script.
 * 
 * @author Leo Soto
 */
public class UniversalRunner {

    /**
     * This is the interface users will be using.
     * 
     * @return the appropriate runner based on the nature type.
     */
    public static AbstractRunner getRunner(IPythonNature nature) {
        try {
            int interpreterType = nature.getInterpreterType();
            switch (interpreterType) {
                case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                    return new PythonRunner(nature);
                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                    return new JythonRunner(nature);
                case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                    return new IronPythonRunner(nature);
                default:
                    throw new RuntimeException("Interpreter type " + interpreterType + "not recognized");
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Abstract runner. Clients must override the way to provide the command line to be run.
     */
    public static abstract class AbstractRunner {

        protected IPythonNature nature;

        public AbstractRunner(IPythonNature nature) {
            this.nature = nature;
        }

        /**
         * Subclasses must override to provide the appropriate command line.
         */
        public abstract String[] getCommandLine(List<String> argumentsAfterPython);

        /**
         * Runs the code and returns its output.
         * 
         * @return a tuple with stdout and stderr
         */
        public Tuple<String, String> runCodeAndGetOutput(String code, String[] args, File workingDir,
                IProgressMonitor monitor) {

            if (args == null) {
                args = new String[0];
            }
            List<String> cmd = new ArrayList<String>();
            cmd.add("-c");
            cmd.add(code);
            cmd.addAll(Arrays.asList(args));

            // We just hope this sets the right env. But looks like it ignores
            // the interpreter env variables (IInterpreterInfo#getEnvVariables)
            return new SimpleRunner().runAndGetOutput(getCommandLine(cmd), workingDir, nature, monitor, null);

        }

        /**
         * Runs the script and returns its output.
         * 
         * @return a tuple with stdout and stderr
         */
        public Tuple<String, String> runScriptAndGetOutput(String script, String[] args, File workingDir,
                IProgressMonitor monitor) {

            if (args == null) {
                args = new String[0];
            }
            List<String> cmd = new ArrayList<String>();
            cmd.add(script);
            cmd.addAll(Arrays.asList(args));

            // We just hope this sets the right env. But looks like it ignores
            // the interpreter env variables (IInterpreterInfo#getEnvVariables)
            return new SimpleRunner().runAndGetOutput(getCommandLine(cmd), workingDir, nature, monitor, null);

        }

        public Tuple<Process, String> createProcess(String script, String[] args, File workingDir,
                IProgressMonitor monitor) {
            File file = new File(script);
            if (!file.exists()) {
                throw new RuntimeException("The script passed for execution (" + script + ") does not exist.");
            }
            if (args == null) {
                args = new String[0];
            }

            List<String> cmd = new ArrayList<String>();
            cmd.add(script);
            cmd.addAll(Arrays.asList(args));

            return new SimpleRunner().run(getCommandLine(cmd), workingDir, nature, monitor);

        }
    }

    /**
     * Provides the command line needed for running a python script
     */
    public static class PythonRunner extends AbstractRunner {
        public PythonRunner(IPythonNature nature) {
            super(nature);
        }

        @Override
        public String[] getCommandLine(List<String> argumentsAfterPython) {
            String interpreter;
            try {
                interpreter = nature.getProjectInterpreter().getExecutableOrJar();
            } catch (Exception e) {
                throw new RuntimeException("Can't get the interpreter", e);
            }
            argumentsAfterPython.add(0, "-u");
            argumentsAfterPython.add(0, interpreter);
            return argumentsAfterPython.toArray(new String[argumentsAfterPython.size()]);
        }
    }

    /**
     * Provides the command line needed for running a jython script
     */
    public static class JythonRunner extends AbstractRunner {

        public JythonRunner(IPythonNature nature) {
            super(nature);
        }

        @Override
        public String[] getCommandLine(List<String> argumentsAfterPython) {
            try {
                return SimpleJythonRunner.makeExecutableCommandStr(nature.getProjectInterpreter().getExecutableOrJar(),
                        argumentsAfterPython.get(0), "", argumentsAfterPython.subList(1, argumentsAfterPython.size())
                                .toArray(new String[0]));
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    /**
     * Provides the command line needed for running an ironpython script
     */
    public static class IronPythonRunner extends AbstractRunner {

        public IronPythonRunner(IPythonNature nature) {
            super(nature);
        }

        @Override
        public String[] getCommandLine(List<String> argumentsAfterPython) {
            try {
                argumentsAfterPython.add(0, "-u");
                return SimpleIronpythonRunner.preparePythonCallParameters(nature.getProjectInterpreter()
                        .getExecutableOrJar(), argumentsAfterPython.get(0),
                        argumentsAfterPython.subList(1, argumentsAfterPython.size()).toArray(new String[0]), false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
