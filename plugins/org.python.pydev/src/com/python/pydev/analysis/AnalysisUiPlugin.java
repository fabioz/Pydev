package com.python.pydev.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.plugin.PydevPlugin;

public class AnalysisUiPlugin {

    private static IDialogSettings dialogSettings = null;

    private static final String FN_DIALOG_SETTINGS = "dialog_settings.xml"; //$NON-NLS-1$

    public static IDialogSettings getDialogSettings() {
        if (dialogSettings == null) {
            loadDialogSettings();
        }
        return dialogSettings;
    }

    /**
     * FOR INTERNAL WORKBENCH USE ONLY.
     *
     * Returns the path to a location in the file system that can be used
     * to persist/restore state between workbench invocations.
     * If the location did not exist prior to this call it will  be created.
     * Returns <code>null</code> if no such location is available.
     *
     * @return path to a location in the file system where this plug-in can
     * persist data between sessions, or <code>null</code> if no such
     * location is available.
     * @since 3.1
     */
    private static IPath getStateLocationOrNull() {
        try {
            return AnalysisPlugin.getDefault().getStateLocation();
        } catch (IllegalStateException e) {
            // This occurs if -data=@none is explicitly specified, so ignore this silently.
            // Is this OK? See bug 85071.
            return null;
        }
    }

    protected static void loadDialogSettings() {
        dialogSettings = new DialogSettings("Workbench"); //$NON-NLS-1$

        // bug 69387: The instance area should not be created (in the call to
        // #getStateLocation) if -data @none or -data @noDefault was used
        IPath dataLocation = getStateLocationOrNull();
        if (dataLocation != null) {
            // try r/w state area in the local file system
            String readWritePath = dataLocation.append(FN_DIALOG_SETTINGS)
                    .toOSString();
            File settingsFile = new File(readWritePath);
            if (settingsFile.exists()) {
                try {
                    dialogSettings.load(readWritePath);
                } catch (IOException e) {
                    // load failed so ensure we have an empty settings
                    dialogSettings = new DialogSettings("Workbench"); //$NON-NLS-1$
                }

                return;
            }
        }

        // otherwise look for bundle specific dialog settings
        URL dsURL = FileLocator.find(AnalysisPlugin.getDefault().getBundle(), new Path(FN_DIALOG_SETTINGS), null);
        if (dsURL == null) {
            return;
        }

        InputStream is = null;
        try {
            is = dsURL.openStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
            dialogSettings.load(reader);
        } catch (IOException e) {
            // load failed so ensure we have an empty settings
            dialogSettings = new DialogSettings("Workbench"); //$NON-NLS-1$
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    public static IPreferenceStore getPreferenceStore() {
        // Create the preference store lazily.
        return PydevPlugin.getDefault().getPreferenceStore();
    }

}
