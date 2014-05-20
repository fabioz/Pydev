package org.python.pydev.plugin.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.BackingStoreException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.dialogs.DialogMemento;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * @author fabioz
 */
public class CheckDefaultPreferencesDialog extends TrayDialog {

    private Button okButton;
    private Button cancelButton;
    private DialogMemento memento;
    private final CheckInfo[] missing;
    List<Button> checkBoxes;

    CheckDefaultPreferencesDialog(Shell shell, CheckInfo[] missing) {
        super(shell);
        setHelpAvailable(false);
        memento = new DialogMemento(shell, "org.python.pydev.plugin.preferences.CheckDefaultPreferencesDialog");
        this.missing = missing;
        checkBoxes = new ArrayList<>(missing.length + 2);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    public boolean close() {
        memento.writeSettings(getShell());
        return super.close();
    }

    @Override
    protected Point getInitialSize() {
        return memento.getInitialSize(super.getInitialSize(), getShell());
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        memento.readSettings();
        Composite area = (Composite) super.createDialogArea(parent);

        createLabel(area, "Uncheck settings that should not be changed.\n");

        for (CheckInfo c : this.missing) {
            Button bt = addCheckBox(area, c.msg, c.description);
            bt.setData(c);
        }

        createLabel(area, "\n"); //Just add spacing
        Button bt = addCheckBox(area, "Re-check whenever a PyDev editor is opened?", "");
        bt.setSelection(PydevRootPrefs.getCheckPreferredPydevSettings());
        bt.setData(PydevRootPrefs.CHECK_PREFERRED_PYDEV_SETTINGS);

        return area;
    }

    private void createLabel(Composite area, String text) {
        Label label = new Label(area, SWT.NONE);
        label.setText(text);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        label.setLayoutData(data);
    }

    private Button addCheckBox(Composite area, String msg, String desc) {
        Button bt = new Button(area, SWT.CHECK);
        bt.setText(msg);
        bt.setSelection(true);

        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        bt.setLayoutData(data);
        checkBoxes.add(bt);

        if (desc.length() > 0) {
            Label label = new Label(area, SWT.NONE);
            label.setText(desc);
            data = new GridData(GridData.FILL_HORIZONTAL);
            data.grabExcessHorizontalSpace = true;
            label.setLayoutData(data);
        }

        return bt;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Default Eclipse preferences for PyDev");
    }

    @Override
    protected void okPressed() {
        applyChanges();
        super.okPressed();
    }

    public static class CheckInfo {

        public final String description;
        public final String plugin;
        public final String pref;
        public final String value;
        public final String msg;

        private CheckInfo(String plugin, String pref, String value, String msg, String description) {
            this.plugin = plugin;
            this.pref = pref;
            this.value = value;
            this.msg = msg;
            this.description = description;
        }

        public static CheckInfo[] getMissing() {
            String buildDesc = "\tReason: To launch a file in PyDev a build is not required.\n\n"
                    + "\tNote that JDT/CDT may require a build to compile before launching,\n"
                    + "\tso, leave it unchecked if you use one of those plugins and want to\n"
                    + "\tautomatically build before launching.";

            CheckInfo[] infos = new CheckInfo[] {
                    new CheckInfo("org.eclipse.debug.ui", "org.eclipse.debug.ui.wait_for_build", "never",
                            "Wait for ongoing build before launching: never", buildDesc),

                    new CheckInfo(
                            "org.eclipse.debug.ui",
                            "org.eclipse.debug.ui.build_before_launch",
                            "false",
                            "Build (if required) before launching: no",
                            buildDesc),

                    new CheckInfo(
                            "org.eclipse.debug.ui",
                            "org.eclipse.debug.ui.UseContextualLaunch",
                            "",
                            "Launch operation: always launch the previously selected application.",
                            "\tReason: On PyDev, F9 launches the current selection (and Ctrl+F9 launches unit-tests),\n"
                                    + "\tso it's recommended that Ctrl+F11 is set to re-launch the last launch and\n"
                                    + "\tF11 to debug the last launch."
                    )
            };
            ArrayList<CheckInfo> lst = new ArrayList<>(infos.length);

            for (CheckInfo c : infos) {
                IEclipsePreferences node = InstanceScope.INSTANCE.getNode(c.plugin);
                if (!c.value.equals(node.get(c.pref, ""))) {
                    lst.add(c);
                }
            }
            return lst.toArray(new CheckInfo[lst.size()]);
        }

        public void apply() {
            IEclipsePreferences node = InstanceScope.INSTANCE.getNode(this.plugin);
            node.put(this.pref, this.value);
            try {
                node.flush();
            } catch (BackingStoreException e) {
                Log.log(e);
            }
        }
    }

    private void applyChanges() {
        for (Button bt : checkBoxes) {
            Object data = bt.getData();
            if (data instanceof CheckInfo) {
                if (bt.getSelection()) {
                    CheckInfo checkInfo = (CheckInfo) data;
                    checkInfo.apply();
                }

            } else if (data.equals(PydevRootPrefs.CHECK_PREFERRED_PYDEV_SETTINGS)) {
                PydevRootPrefs.setCheckPreferredPydevSettings(bt.getSelection());

            } else {
                Log.log("Unexpected data: " + data);
            }
        }
    }

    public static void askAboutSettings() {
        IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
        boolean checkPreferredSettings = preferenceStore.getBoolean(PydevRootPrefs.CHECK_PREFERRED_PYDEV_SETTINGS);
        if (checkPreferredSettings) {
            final CheckInfo[] missing = CheckInfo.getMissing();
            if (missing.length == 0) {
                return;
            }

            boolean runNowIfInUiThread = true;
            RunInUiThread.async(new Runnable() {

                @Override
                public void run() {
                    Shell shell = EditorUtils.getShell();
                    CheckDefaultPreferencesDialog dialog = new CheckDefaultPreferencesDialog(shell, missing);
                    dialog.open();
                }
            }, runNowIfInUiThread);

        }
    }

}
