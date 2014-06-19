package org.python.pydev.shared_ui.debug;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.actions.BaseAction;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;

@SuppressWarnings("restriction")
public class RestartLaunchAction extends BaseAction implements IUpdate, IEditorActionDelegate {

    protected IPageBookViewPage page;
    protected ProcessConsole console;
    private final ILaunch launch;
    private final ILaunchConfiguration launchConfiguration;

    private static ILaunch lastLaunch;
    private static ILaunchConfiguration lastConfig;

    public RestartLaunchAction(IPageBookViewPage page, ProcessConsole console) {
        this.page = page;
        this.console = console;
        launch = this.console.getProcess().getLaunch();
        launchConfiguration = launch.getLaunchConfiguration();

        lastLaunch = launch;
        lastConfig = launch.getLaunchConfiguration();

        update();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.IUpdate#update()
     */
    public void update() {
        IProcess process = console.getProcess();
        setEnabled(true);
        KeySequence binding = KeyBindingHelper
                .getCommandKeyBinding("org.python.pydev.debug.ui.actions.relaunchLastAction");
        String str = binding != null ? "(" + binding.format() + " with focus on editor)" : "(unbinded)";
        if (process.canTerminate()) {
            this.setImageDescriptor(SharedUiPlugin.getImageCache().getDescriptor(UIConstants.RELAUNCH));
            this.setToolTipText("Restart the current launch. " + str);

        } else {
            this.setImageDescriptor(SharedUiPlugin.getImageCache().getDescriptor(UIConstants.RELAUNCH1));
            this.setToolTipText("Relaunch with the same configuration." + str);
        }
    }

    public static void relaunch(ILaunch launch, ILaunchConfiguration launchConfiguration) {
        if (launch != null && launchConfiguration != null) {
            try {
                launch.terminate();
            } catch (DebugException e) {
                Log.log(e);
            }
            try {
                launchConfiguration.launch(launch.getLaunchMode(), null);
            } catch (CoreException e) {
                Log.log(e);
            }
        }
    }

    @Override
    public void run(IAction action) {
        relaunch(launch, launchConfiguration);
    }

    @Override
    public void run() {
        run(this);
    }

    public void dispose() {
        this.page = null;
        this.console = null;
    }

    public static void relaunchLast() {
        relaunch(lastLaunch, lastConfig);
    }

}
