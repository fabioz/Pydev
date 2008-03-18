/*
 * Created on Mar 20, 2006
 */
package org.python.pydev.debug.newconsole.env;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;

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
            Composite area = (Composite) super.createDialogArea(parent);

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

        public boolean isExternalJythonProcess() {
            return isExt;
        }
    }

    /**
     * @return a shell that we can use.
     */
    public Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }


    public ILaunch createILaunch(IProject project, File toLaunch, IInterpreterManager manager, String arguments)
            throws UserCanceledException, CoreException {
        String type = null;

        String interactiveConsoleVmArgs = null;
        if (manager.isPython()) {
            type = "org.python.pydev.debug.regularLaunchConfigurationType";
        } else if (manager.isJython()) {
            //if it is jython, we have to check with the user if it wants a process attached to Eclipse or an external process.
            Display display = Display.getDefault();
            final ChooseJythonProcessTypeDialog dialog = new ChooseJythonProcessTypeDialog(getShell());
            display.syncExec(new Runnable() {
                public void run() {
                    dialog.open();
                }
            });

            if (dialog.getReturnCode() != Dialog.OK) {
                throw new UserCanceledException("User cancelled action.");
            }
            if (dialog.isExternalJythonProcess()) {
//                interactiveConsoleVmArgs = InteractiveConsolePreferencesPage.getInteractiveConsoleVmArgs();
                type = "org.python.pydev.debug.jythonLaunchConfigurationType";
            } else {
                type = "internalJython";
            }
        } else {
            throw new RuntimeException("Unable to determine its launch type.");
        }

        ILaunch launch = null;
        if (type.equals("internalJython")) {
//            return new JythonInternalProcess(edit);
            throw new RuntimeException("Not handled now!");

        } else {
            ILaunchConfiguration configuration = AbstractLaunchShortcut.createDefaultLaunchConfiguration(null, type,
                    toLaunch.getAbsolutePath(), manager, project.getName(), interactiveConsoleVmArgs, arguments, false);

            launch = configuration.launch("interactive", new NullProgressMonitor());
        }
        return launch;
    }


}
