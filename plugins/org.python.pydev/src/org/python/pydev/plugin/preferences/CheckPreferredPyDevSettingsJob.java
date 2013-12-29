package org.python.pydev.plugin.preferences;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.plugin.preferences.CheckDefaultPreferencesDialog.CheckInfo;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.utils.RunInUiThread;

public class CheckPreferredPyDevSettingsJob extends Job {

    public CheckPreferredPyDevSettingsJob() {
        super("Check Preferred PyDev Settings");
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        CheckInfo[] missing = CheckInfo.getMissing();
        if (missing.length == 0) {
            return Status.OK_STATUS;
        }

        RunInUiThread.async(new Runnable() {

            @Override
            public void run() {
                Shell shell = EditorUtils.getShell();
                CheckInfo[] missing = CheckInfo.getMissing();
                CheckDefaultPreferencesDialog dialog = new CheckDefaultPreferencesDialog(shell, missing);
                dialog.open();
            }
        });
        return Status.OK_STATUS;
    }

}
