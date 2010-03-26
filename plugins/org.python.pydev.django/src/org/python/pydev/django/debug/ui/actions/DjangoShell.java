package org.python.pydev.django.debug.ui.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.Launch;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.Tuple4;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.debug.newconsole.PydevConsoleInterpreter;
import org.python.pydev.debug.newconsole.env.IProcessFactory;
import org.python.pydev.django.launching.DjangoConstants;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

public class DjangoShell extends DjangoAction {

    public void run(IAction action) {
    	try {
//   		 this.launchDjangoCommand("shell", false);
    		
    		
    		PythonNature nature = PythonNature.getPythonNature(selectedProject);
    		if(nature == null){
    			MessageDialog.openError(
    					PyAction.getShell(), 
    					"Pydev nature not found", 
    					"Unable to perform action because the Pydev nature is not properly set.");
    			return;
    		}
    		IPythonPathNature pythonPathNature = nature.getPythonPathNature();
    		String settingsModule = null;
    		Map<String, String> variableSubstitution = null;
    		try {
    			variableSubstitution = pythonPathNature.getVariableSubstitution();
    			settingsModule = variableSubstitution.get(DjangoConstants.DJANGO_SETTINGS_MODULE);
    		} catch (Exception e1) {
    			throw new RuntimeException(e1);
    		}
    		if(settingsModule == null){
                InputDialog d = new InputDialog(
                		PyAction.getShell(), 
                		"Settings module", 
                		"Please enter the settings module to be used.\n" +
                		"\n" +
                		"Note that it can be edited later in:\np" +
                		"roject properties > pydev pythonpath > string substitution variables.", 
                		selectedProject.getName()+".settings", 
                		new IInputValidator(){
                    
                    public String isValid(String newText) {
                    	if(newText.length() == 0){
                    		return "Text must be entered.";
                    	}
                        for(char c:newText.toCharArray()){
                        	if(c == ' '){
                        		return "Whitespaces not accepted";
                        	}
                        	if(c != '.' && !Character.isJavaIdentifierPart(c)){
                        		return "Invalid char: "+c;
                        	}
                        }
                        return null;
                }});

                int retCode = d.open();
                
                if (retCode == InputDialog.OK) {
                	settingsModule = d.getValue();
    				variableSubstitution.put(DjangoConstants.DJANGO_SETTINGS_MODULE, settingsModule);
    				try {
    					pythonPathNature.setVariableSubstitution(variableSubstitution);
    				} catch (Exception e) {
    					PydevPlugin.log(e);
    				}

                }
                	
                	
    			if(settingsModule == null){
    				return;
    			}
    		}
    		
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
    		
    		String importStr = "";//"from " + selectedProject.getName() + " import settings;";
			importStr = "import "+settingsModule+" as settings;";
    		
    		
    		consoleFactory.createConsole(
    				interpreter, 
    				"\nfrom django.core import management;" +
    				importStr +
    				"management.setup_environ(settings)\n"
    				);
    		
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

}
