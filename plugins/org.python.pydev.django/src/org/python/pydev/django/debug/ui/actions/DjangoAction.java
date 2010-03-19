package org.python.pydev.django.debug.ui.actions;
import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.views.console.ProcessConsoleManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.django.launching.DjangoConstants;
import org.python.pydev.django.launching.PythonFileRunner;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Base class for django actions.
 */
public abstract class DjangoAction implements IObjectActionDelegate {

    /**
     * The project that was selected (may be null).
     */
    protected IProject selectedProject;

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // empty
    }

    /**
     * Actually remove the python nature from the project.
     */
    public abstract void run(IAction action);

    /**
     * A project was just selected
     */
    public void selectionChanged(IAction action, ISelection selection) {
        selectedProject = null;
        
        if (selection.isEmpty() || !(selection instanceof IStructuredSelection)) {
            return;
        }
        
        IStructuredSelection selections = (IStructuredSelection) selection;
        Object project = selections.getFirstElement();
        if(!(project instanceof IProject)){
            return;
        }
        
        this.selectedProject = (IProject) project;
    }
	
    /**
     * May be used to run some command that uses the manage.py file.
     */
	@SuppressWarnings("restriction")
	public ILaunch launchDjangoCommand(final String command, boolean refreshAndShowMessageOnFinish) {
		PythonNature nature = PythonNature.getPythonNature(selectedProject);
		String manageVarible = null;
		try {
			Map<String, String> variableSubstitution = nature.getPythonPathNature().getVariableSubstitution();
			manageVarible = variableSubstitution.get(DjangoConstants.DJANGO_MANAGE_VARIABLE);
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
		if(manageVarible == null){
			throw new RuntimeException("Unable to make launch because the variable: "+
					DjangoConstants.DJANGO_MANAGE_VARIABLE+" is not declared in the project.");
		}
		IFile manageDotPy = selectedProject.getFile(manageVarible);
		if(manageDotPy == null || !manageDotPy.exists()){
			throw new RuntimeException("Unable to make launch because the manage.py was not found at: "+
					manageVarible + " in "+selectedProject.getName());
		}
		try {
			ILaunch launch = PythonFileRunner.launch(manageDotPy, command);
			
			//After the command completes, refresh and put message for user.
    		final IProcess[] processes = launch.getProcesses();
    		ProcessConsoleManager consoleManager = DebugUIPlugin.getDefault().getProcessConsoleManager();
    		if(processes.length >= 1){
	    		IConsole console = consoleManager.getConsole(processes[0]);
	    		final IOConsoleOutputStream outputStream = ((IOConsole)console).newOutputStream();
	    		
	    		new Job("Refresh on finish") {
					
					protected IStatus run(IProgressMonitor monitor) {
						boolean allTerminated = false;
						while(!allTerminated){
							allTerminated = true;
							for(IProcess p:processes){
								if(!p.isTerminated()){
									allTerminated = false;
									break;
								}
							}
							
						}
						try {
							outputStream.write(StringUtils.format("Terminated: manage.py "+ command));
						} catch (IOException e1) {
							//ignore
							e1.printStackTrace();
						}
						
						try {
							outputStream.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						try {
							selectedProject.refreshLocal(IResource.DEPTH_INFINITE, null);
						} catch (CoreException e) {
							PydevPlugin.log(e);
						}
	
						return Status.OK_STATUS;
					}
				}.schedule();
    		}
    		return launch;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
