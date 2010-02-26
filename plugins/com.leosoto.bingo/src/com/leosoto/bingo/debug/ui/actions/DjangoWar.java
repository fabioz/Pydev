package com.leosoto.bingo.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.SimpleRunner;


public class DjangoWar extends DjangoAction {

    public void run(IAction action) {
    	try {
	    	PythonNature nature = PythonNature.getPythonNature(selectedProject);
	    	if (nature.getInterpreterType() != IInterpreterManager.INTERPRETER_TYPE_JYTHON) {
	    		MessageDialog.openInformation(
	    				null, "Can't create WAR", 
                        "Creation of WAR packages is only supported on Jython");
	    		return;
	    	}
	    	String projectPythonPath = 
	    		nature.getPythonPathNature().getOnlyProjectPythonPathStr();
	    	String javaLibs = null;
	    	for (String path: projectPythonPath.split("\\|")) {
	    		if (path.endsWith(".jar")) {
	    			if (javaLibs == null) {
	    				javaLibs = path;
	    			} else { 
		    			javaLibs += SimpleRunner.getPythonPathSeparator();
		    			javaLibs += path;
	    			}
	    		}
	    	}    	
    		String command = "war";
    		if (javaLibs != null) {
    			command += " --include-java-libs=" + javaLibs;
    		}
    		launchDjangoCommand(command);
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
}
