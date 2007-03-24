package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * Action to enable/disable a breakpoint in the ruler.
 * 
 * @author Fabio
 */
public class EnableDisableBreakpointRulerAction extends AbstractBreakpointRulerAction {

    public EnableDisableBreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
        fInfo = rulerInfo;
        fTextEditor = editor;
    }

    public void update() {
        setBreakpoint(determineBreakpoint());
        if (getBreakpoint() == null) {
            setEnabled(false);
            return;
        }
        setEnabled(true);
        try {
            boolean enabled = getBreakpoint().isEnabled();
            setText(enabled ? "&Disable Breakpoint" : "&Enable Breakpoint");
        } catch (CoreException ce) {
            PydevDebugPlugin.log(IStatus.ERROR, ce.getLocalizedMessage(), ce);
        }
    }

    @Override
    public void run() {

        if (getBreakpoint() != null) {
            new Job("Enabling / Disabling Breakpoint") { //$NON-NLS-1$
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        getBreakpoint().setEnabled(!getBreakpoint().isEnabled());
                        return Status.OK_STATUS;
                    } catch (final CoreException e) {
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                ErrorDialog.openError(getTextEditor().getEditorSite().getShell(), "Enabling/disabling breakpoints",
                                        "Exceptions occurred enabling disabling the breakpoint", e.getStatus());
                            }
                        });
                    }
                    return Status.CANCEL_STATUS;
                }
            }.schedule();
        }
    }
}
