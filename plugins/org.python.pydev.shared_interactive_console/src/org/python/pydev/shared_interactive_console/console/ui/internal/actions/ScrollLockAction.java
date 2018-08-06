package org.python.pydev.shared_interactive_console.console.ui.internal.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;

public class ScrollLockAction extends Action {

    private ScriptConsole console;

    public ScrollLockAction(ScriptConsole console, String text, String tooltip) {
        this.console = console;
        setText(text);
        setToolTipText(tooltip);
        update();
    }

    @Override
    public void run() {
        console.getViewer().setScrollLock(isChecked());
        update();
    }

    private void update() {
        setChecked(console.getViewer().getScrollLock());
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        try {
            return ImageDescriptor
                    .createFromURL(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/elcl16/lock_co.png"));
        } catch (MalformedURLException e) {
            Log.log(e);
            return null;
        }
    }

}
