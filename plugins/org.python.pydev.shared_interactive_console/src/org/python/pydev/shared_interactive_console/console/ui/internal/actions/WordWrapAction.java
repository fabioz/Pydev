package org.python.pydev.shared_interactive_console.console.ui.internal.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.StyledText;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;

public class WordWrapAction extends Action {

    private ScriptConsole console;

    public WordWrapAction(ScriptConsole console, String text, String tooltip) {
        this.console = console;
        setText(text);
        setToolTipText(tooltip);
        update();
    }

    @Override
    public void run() {
        StyledText control = getStyledText();
        control.setWordWrap(!control.getWordWrap());
        update();
    }

    private void update() {
        setChecked(getStyledText().getWordWrap());
    }

    private StyledText getStyledText() {
        return (StyledText) console.getViewer().getControl();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        try {
            return ImageDescriptor
                    .createFromURL(new URL("platform:/plugin/org.eclipse.ui.console/icons/full/elcl16/wordwrap.png"));
        } catch (MalformedURLException e) {
            Log.log(e);
            return null;
        }
    }

}
