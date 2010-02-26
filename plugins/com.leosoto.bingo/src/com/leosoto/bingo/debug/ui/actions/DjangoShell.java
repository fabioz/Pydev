package com.leosoto.bingo.debug.ui.actions;

import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.Launch;
import org.eclipse.jface.action.IAction;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple3;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.debug.newconsole.PydevConsoleInterpreter;
import org.python.pydev.debug.newconsole.env.IProcessFactory;
import org.python.pydev.dltk.console.InterpreterResponse;
import org.python.pydev.plugin.nature.PythonNature;

public class DjangoShell extends DjangoAction {

    public void run(IAction action) {
    	try {
    		PythonNature nature = PythonNature.getPythonNature(selectedProject);
    		List<IPythonNature> natures = Collections.singletonList((IPythonNature)nature);
    		// DjangoManagementRunner.launch(selectedProject, "shell");
    		PydevConsoleFactory consoleFactory = new PydevConsoleFactory();
    		Tuple3<Launch, Process, Integer> launchAndProcess =
    			new IProcessFactory().createLaunch(
    					nature.getRelatedInterpreterManager(),
    					nature.getProjectInterpreter(), 
    							nature.getPythonPathNature()
    							      .getCompleteProjectPythonPath(
    							    		  nature.getProjectInterpreter(),
    							    		  nature.getRelatedInterpreterManager()),    					
    					nature,
    					natures);
    		PydevConsoleInterpreter interpreter = 
    			PydevConsoleFactory.createPydevInterpreter(
					launchAndProcess.o1, launchAndProcess.o2,
					launchAndProcess.o3, natures);
    		consoleFactory.createConsole(interpreter);
    		interpreter.exec(
    				"from django.core import management;" +
    				"from " + selectedProject.getName() + " import settings;" +
    				"management.setup_environ(settings)", 
    				new ICallback<Object, InterpreterResponse>() {
						@Override
						public Object call(InterpreterResponse arg) {
							return null;
						}
					});
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

}
