package org.python.pydev.django.debug.ui.actions;

import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.Launch;
import org.eclipse.jface.action.IAction;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple4;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.debug.newconsole.PydevConsoleInterpreter;
import org.python.pydev.debug.newconsole.env.IProcessFactory;
import org.python.pydev.plugin.nature.PythonNature;

public class DjangoShell extends DjangoAction {

    public void run(IAction action) {
    	try {
//   		 this.launchDjangoCommand("shell", false);
    		
    		PythonNature nature = PythonNature.getPythonNature(selectedProject);
    		List<IPythonNature> natures = Collections.singletonList((IPythonNature)nature);
    		PydevConsoleFactory consoleFactory = new PydevConsoleFactory();
    		Tuple4<Launch, Process, Integer, IInterpreterInfo> launchAndProcess =
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
					launchAndProcess.o3, launchAndProcess.o4, natures);
    		
    		consoleFactory.createConsole(
    				interpreter, 
    				"\nfrom django.core import management;" +
    				"from " + selectedProject.getName() + " import settings;" +
    				"management.setup_environ(settings)\n"
    				);
    		
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

}
