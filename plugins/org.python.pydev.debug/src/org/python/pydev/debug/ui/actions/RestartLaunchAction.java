package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

@SuppressWarnings("restriction")
public class RestartLaunchAction extends PyAction implements IUpdate{

    protected IPageBookViewPage page;
    protected ProcessConsole console;

    public RestartLaunchAction(IPageBookViewPage page, ProcessConsole console) {
        this.page = page;
        this.console = console;
        update();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.IUpdate#update()
     */
    public void update() {
        IProcess process = console.getProcess(); 
        setEnabled(true);
        if(process.canTerminate()){
            this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.RELAUNCH));
            this.setToolTipText("Restart the current launch.");
            
        }else{
            this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.RELAUNCH1));
            this.setToolTipText("Relaunch with the same configuration.");
        }
    }
    
    public void run(IAction action) {
        ILaunch launch = this.console.getProcess().getLaunch();
        try {
            launch.terminate();
        } catch (DebugException e) {
            PydevPlugin.log(e);
        }
        ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
        try {
            launchConfiguration.launch(launch.getLaunchMode(), null);
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
    }
    
    public void run() {
        run(this);
    }

    public void dispose() {
        this.page = null;
        this.console = null;
    }

}
