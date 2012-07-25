package org.python.pydev.dltk.console.ui.internal.actions;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.python.pydev.dltk.console.ui.ScriptConsole;
import org.python.pydev.dltk.console.ui.ScriptConsoleManager;
import org.python.pydev.dltk.console.ui.ScriptConsoleUIConstants;
import org.python.pydev.plugin.PydevPlugin;

public class LinkWithDebugSelectionAction extends Action {

    private ScriptConsole console;

    public LinkWithDebugSelectionAction(ScriptConsole console, String text, String tooltip) {
        super(text, IAction.AS_CHECK_BOX);
        this.console = console;
        setToolTipText(tooltip);
        setImageDescriptor(getImageDescriptor());
        setDisabledImageDescriptor(getImageDescriptor());
        setText(text);
        // set true by default
        setChecked(true);
    }

    public ImageDescriptor getImageDescriptor() {
        String imagePath = ScriptConsoleUIConstants.ICONS_PATH + File.separator
                + ScriptConsoleUIConstants.LINK_WITH_DEBUGGER;
        return ImageDescriptor.createFromImage(PydevPlugin.getImageCache().get(imagePath));
    }

    public void run() {
        boolean isChecked = isChecked();
        ScriptConsoleManager.getInstance().linkWithDebugSelection(console, isChecked);
    }

    public void update() {
        setEnabled(true);
    }
}
