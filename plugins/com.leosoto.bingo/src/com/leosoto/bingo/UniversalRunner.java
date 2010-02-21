package com.leosoto.bingo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.copiedfromeclipsesrc.JavaVmLocationFinder;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.SimpleRunner;

public class UniversalRunner {

	public static abstract class Runner {
		public abstract String[] getInterpreterCommandLine(IProject project);
		public Tuple<String, String> runCodeAndGetOutput(
				IProject project, String code, String[] args, File workingDir,
				IProgressMonitor monitor) {

	        if (args == null) {
	            args = new String[0];
	        }
	        String[] interpreter = getInterpreterCommandLine(project);
	        SimpleRunner runner = new SimpleRunner(); // To avoid too much code
	                                                  // duplication. SimpleRunner
	                                                  // code should be moved
	                        						  // to another class perhaps?
	        List<String> cmd = new ArrayList<String>();
	        cmd.addAll(Arrays.asList(interpreter));
	        cmd.add("-u");
	        cmd.add("-c");
	        cmd.add(code);
	        cmd.addAll(Arrays.asList(args));
	        // We just hope this sets the right env. But looks like it ignores
	        // the interpreter env variables (IInterpreterInfo#getEnvVariables)
	        return runner.runAndGetOutput(
	        		cmd.toArray(new String[0]), workingDir, project, monitor);

		}
		public Tuple<String, String> runScriptAndGetOutput(
				IProject project, String script, String[] args, File workingDir,
				IProgressMonitor monitor) {

			File file = new File(script);
	        if (!file.exists()) {
	            throw new RuntimeException(
	            		"The script passed for execution ("+script+") does not exist.");
	        }
	        if (args == null) {
	            args = new String[0];
	        }
	        String[] interpreter = getInterpreterCommandLine(project);
	        SimpleRunner runner = new SimpleRunner(); // To avoid too much code
	                                                  // duplication. But if
	                                                  // this refactoring
	                                                  // works, SimpleRunner
	                                                  // code should be moved
	                        						  // to this class
	        List<String> cmd = new ArrayList<String>();
	        cmd.addAll(Arrays.asList(interpreter));
	        cmd.add("-u");
	        cmd.add(script);
	        cmd.addAll(Arrays.asList(args));
	        // We just hope this sets the right env. But looks like it ignores
	        // the interpreter env variables (IInterpreterInfo#getEnvVariables)
	        return runner.runAndGetOutput(
	        		cmd.toArray(new String[0]), workingDir, project, monitor);
		}
	}

	public static class PythonRunner extends Runner {
		@Override
		public String[] getInterpreterCommandLine(IProject project) {
			PythonNature nature = PythonNature.getPythonNature(project);
			String interpreter;
			try {
				interpreter = nature.getProjectInterpreter().getExecutableOrJar();
			} catch (Exception e) {
				throw new RuntimeException("Can't get the interpreter", e);
			}
			return new String[] { interpreter };
		}
	}

	public static class JythonRunner extends Runner {

		@Override
		public String[] getInterpreterCommandLine(IProject project) {
			PythonNature nature = PythonNature.getPythonNature(project);
			String java;
			String jar;
			try {
				java = JavaVmLocationFinder.findDefaultJavaExecutable().getCanonicalPath();
			} catch (Exception e) {
				throw new RuntimeException("Can't get the Java excecutable", e);
			}
			try {
				jar = nature.getProjectInterpreter().getExecutableOrJar();
			} catch (Exception e) {
				throw new RuntimeException("Can't get the interpreter", e);
			}
			return new String[] {java, "-jar", jar};
		}
	}

	public static class IronPythonRunner extends Runner {
		@Override
		public String[] getInterpreterCommandLine(IProject project) {
			PythonNature nature = PythonNature.getPythonNature(project);
			String interpreter;
			try {
				interpreter = nature.getProjectInterpreter().getExecutableOrJar();
			} catch (Exception e) {
				throw new RuntimeException("Can't get the interpreter", e);
			}
			// defaultVmArgs code taken from SimpleIronpythonRunner
	        String defaultVmArgs;
			PydevPlugin plugin = PydevPlugin.getDefault();
	        if(plugin == null){
	            //in tests
	            defaultVmArgs = IInterpreterManager.IRONPYTHON_DEFAULT_INTERNAL_SHELL_VM_ARGS;
	        }else{
	            IPreferenceStore preferenceStore = plugin.getPreferenceStore();
	            defaultVmArgs = preferenceStore.getString(IInterpreterManager.IRONPYTHON_INTERNAL_SHELL_VM_ARGS);
	        }

	        List<String> cmd = new ArrayList<String>();
	        cmd.add(interpreter);
	        if (defaultVmArgs != null){
	            cmd.addAll(StringUtils.split(defaultVmArgs, ' '));
	        }
	        return cmd.toArray(new String[0]);
		}
	}

	public static Runner getRunner(IProject project) {
		try {
			int interpreterType =
				PythonNature.getPythonNature(project).getInterpreterType();
			switch (interpreterType) {
			case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
				return new PythonRunner();
			case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
				return new JythonRunner();
			case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
				return new IronPythonRunner();
			default:
				throw new RuntimeException(
						"Interpreter type " + interpreterType + "not recognized");
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public static Tuple<String, String> runCodeAndGetOutput(
			IProject project, String code, String[] args, File workingDir,
			IProgressMonitor monitor) {
		return getRunner(project).runCodeAndGetOutput(project, code, args, workingDir, monitor);
	}


}
