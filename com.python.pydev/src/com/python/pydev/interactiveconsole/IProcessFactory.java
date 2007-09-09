/*
 * Created on Mar 20, 2006
 */
package com.python.pydev.interactiveconsole;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IOConsole;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.editor.PyEdit;

/**
 * This class is used to create the given IProcess and get the console that is attached to that process. 
 */
public class IProcessFactory {

    /**
     * Helper to choose which kind of jython run will it be.
     */
    protected final static class ChooseJythonProcessTypeDialog extends Dialog {
        private Button checkbox1;
        private Button checkbox2;
        private boolean isExt;

        protected ChooseJythonProcessTypeDialog(Shell shell) {
            super(shell);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite area= (Composite)super.createDialogArea(parent);
            
            checkbox1 = new Button(area, SWT.RADIO);
            checkbox1.setText("External Jython Process");
            checkbox2 = new Button(area, SWT.RADIO);
            checkbox2.setText("Eclipse-attached Jython Process");
            return area;
        }

        
        @Override
        protected void okPressed() {
            isExt = checkbox1.getSelection();
            super.okPressed();
        }
        
        public boolean isExternalJythonProcess(){
            return isExt;
        }
    }

    /**
     * @return a shell that we can use.
     */
    public Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    /**
     * Create an IProcess given the input it gets
     * 
     * @param project the project the lauch is associated with
     * @param resource the resource the launch is attached to
     * @param manager the manager related to the new process
     * @return an IProcess given the input configurations passed
     * @throws CoreException
     * @throws UserCanceledException 
     */
    public IProcess createIProcess(IProject project, IResource resource, IInterpreterManager manager, PyEdit edit) throws CoreException, UserCanceledException {
        String type = null;
    
        String interactiveConsoleVmArgs = null;
        if(manager.isPython()){
            type = "org.python.pydev.debug.regularLaunchConfigurationType";
        }else if (manager.isJython()){
            //if it is jython, we have to check with the user if it wants a process attached to Eclipse or an external process.
            Display display = Display.getDefault();
            final ChooseJythonProcessTypeDialog dialog = new ChooseJythonProcessTypeDialog(getShell());
            display.syncExec(new Runnable(){
                public void run() {
                    dialog.open();
                }
            });
            
            if(dialog.getReturnCode() != Dialog.OK){
                throw new UserCanceledException("User cancelled action.");
            }
            if(dialog.isExternalJythonProcess()){
                interactiveConsoleVmArgs = InteractiveConsolePreferencesPage.getInteractiveConsoleVmArgs();
                type = "org.python.pydev.debug.jythonLaunchConfigurationType";
            }else{
                type = "internalJython";
            }
        }else{
            throw new RuntimeException("Unable to determine its launch type.");
        }
        
        if(type.equals("internalJython")){
            return new JythonInternalProcess(edit);
            
        }else{
            ILaunchConfiguration configuration = 
                AbstractLaunchShortcut.createDefaultLaunchConfiguration(
                        new IResource[]{resource}, 
                        type, 
                        AbstractLaunchShortcut.getDefaultLocation(new IResource[]{resource}), 
                        manager, 
                        project.getName(), 
                        interactiveConsoleVmArgs
                        );
            
            ILaunch launch = configuration.launch("interactive", new NullProgressMonitor());
            IProcess[] processes = launch.getProcesses();
            return processes[0];
        }
    }

    /**
     * @return the IOConsole attached to the current process
     */
    public IOConsole getIOConsole(IProcess process) {
        if(process instanceof JythonInternalProcess){
            return ((JythonInternalProcess)process).getIOConsole();
        }
        return (IOConsole) DebugUITools.getConsole(process);
    }

}
