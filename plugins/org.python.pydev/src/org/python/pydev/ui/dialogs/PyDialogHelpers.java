/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListDialog;
import org.python.pydev.ast.interpreter_managers.AbstractInterpreterManager;
import org.python.pydev.ast.interpreter_managers.PyDevCondaPreferences;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.InterpreterGeneralPreferences;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.dialogs.DialogHelpers;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.shared_ui.utils.UIUtils;
import org.python.pydev.ui.pythonpathconf.InterpreterConfigHelpers;
import org.python.pydev.ui.pythonpathconf.NameAndExecutable;
import org.python.pydev.ui.pythonpathconf.conda.CondaConfigDialog;
import org.python.pydev.ui.pythonpathconf.package_manager.CondaPackageManager;

/**
 * @author fabioz
 *
 */
public class PyDialogHelpers {

    public static void openWarning(String title, String message) {
        DialogHelpers.openWarning(title, message);
    }

    public static void openCritical(String title, String message) {
        DialogHelpers.openCritical(title, message);
    }

    public static boolean openQuestion(String title, String message) {
        return DialogHelpers.openQuestion(title, message);
    }

    public static Integer openAskInt(String title, String message, int initial) {
        return DialogHelpers.openAskInt(title, message, initial);
    }

    public static int openWarningWithIgnoreToggle(String title, String message, String key) {
        Shell shell = EditorUtils.getShell();
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String val = store.getString(key);
        if (val.trim().length() == 0) {
            val = MessageDialogWithToggle.PROMPT; //Initial value if not specified
        }

        if (!val.equals(MessageDialogWithToggle.ALWAYS)) {
            MessageDialogWithToggle.openWarning(shell, title, message, "Don't show this message again", false, store,
                    key);
        }
        return MessageDialog.OK;
    }

    public static boolean openQuestionWithIgnoreToggle(String title, String message, String key) {
        Shell shell = EditorUtils.getShell();
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String val = store.getString(key);
        if (val.trim().length() == 0) {
            val = MessageDialogWithToggle.PROMPT; //Initial value if not specified
        }

        if (!val.equals(MessageDialogWithToggle.ALWAYS)) {
            MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(shell, title, message,
                    "Don't show this message again", false, store,
                    key);
            if (dialog.getReturnCode() != IDialogConstants.YES_ID) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the index chosen or -1 if it was canceled.
     */
    public static int openCriticalWithChoices(String title, String message, String[] choices) {
        Shell shell = EditorUtils.getShell();
        MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.ERROR, choices, 0);
        return dialog.open();
    }

    /**
     * @return the index chosen or -1 if it was canceled.
     */
    public static int openQuestionWithChoices(String title, String message, String... choices) {
        Shell shell = EditorUtils.getShell();
        MessageDialog dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION_WITH_CANCEL,
                choices, 0);
        return dialog.open();
    }

    public final static int INTERPRETER_CANCEL_CONFIG = -1;

    private static MessageDialog dialog = null;
    private static int enableAskInterpreter = 0;

    /**
     * Use this to disable/try to enable displaying a "configure interpreter" dialog when an interpreter
     * cannot be found. Disabling it is useful for when it shouldn't be displayed on top of exisitng dialogs.
     * @param enable Set to <code>false</code> to disable the dialogs from appearing, or <code>true</code>
     * to try to re-enable them. The dialogs will only be enabled once all "disable" calls have been negated
     * by an equal number of "enable" calls.
     */
    public static void enableAskInterpreterStep(boolean enable) {
        enableAskInterpreter = Math.min(enableAskInterpreter + (enable ? 1 : -1), 0);
        if (enableAskInterpreter < 0 && dialog != null) {
            dialog.close();
            dialog = null;
        }
    }

    public static int openQuestionConfigureInterpreter(AbstractInterpreterManager m) {
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        String key = InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER + m.getInterpreterType();
        boolean val = store.getBoolean(key);

        if (val) {
            String title = m.getInterpreterUIName() + " not configured";
            String message = "It seems that the " + m.getInterpreterUIName()
                    + " interpreter is not currently configured.\n\nHow do you want to proceed?";
            Shell shell = EditorUtils.getShell();

            String[] dialogButtonLabels = ArrayUtils.concatArrays(
                    InterpreterConfigHelpers.CONFIG_NAMES_FOR_FIRST_INTERPRETER,
                    new String[] { "Don't ask again" });

            dialog = new MessageDialog(shell, title, null, message, MessageDialog.QUESTION,
                    dialogButtonLabels, 0);
            int open = dialog.open();

            //If dialog is null now, it was forcibly closed by a "disable" call of enableAskInterpreterStep.
            if (dialog != null) {
                dialog = null;
                // "Don't ask again" button is the final button in the list
                if (open == dialogButtonLabels.length - 1) {
                    store.setValue(key, false);
                    return INTERPRETER_CANCEL_CONFIG;
                }
                return open;
            }
        }
        return INTERPRETER_CANCEL_CONFIG;
    }

    /**
     * <p>Statically open a list dialog to choose an interpreter from Conda.</p>
     * <p>It returns both the interpreter name and executable.</p>
     *
     * @return NameAndExecutable
     */
    public static NameAndExecutable openCondaInterpreterSelection(Shell parentShell) {
        File condaExe = PyDevCondaPreferences.getExecutable();
        if (condaExe == null) {
            new CondaConfigDialog(parentShell).open();
            condaExe = PyDevCondaPreferences.getExecutable();
            if (condaExe == null) {
                return null;
            }
        }

        List<File> envs = CondaPackageManager.listCondaEnvironments(condaExe);
        List<NameAndExecutable> nameAndExecutableList = getAsNameAndExecutable(envs);
        if (nameAndExecutableList.size() == 0) {
            openWarning("Error", "Could not find any Conda environment to choose from.");
            return null;
        }

        Collections.sort(nameAndExecutableList, new Comparator<NameAndExecutable>() {

            @Override
            public int compare(NameAndExecutable o1, NameAndExecutable o2) {
                return o1.o1.compareToIgnoreCase(o2.o1);
            }
        });

        String title = "Conda interpreter selection";
        String message = "Select an intepreter from the list.";
        LabelProvider labelProvider = new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                return ImageCache.asImage(SharedUiPlugin.getImageCache().get(UIConstants.PY_INTERPRETER_ICON));
            }

            @Override
            public String getText(Object element) {
                if (element != null && element instanceof NameAndExecutable) {
                    NameAndExecutable nameAndExecutable = (NameAndExecutable) element;
                    String name = nameAndExecutable.o1;
                    name = StringUtils.truncateIfNeeded(name, 30);

                    return name
                            + StringUtils.createSpaceString(35 - name.length())
                            + nameAndExecutable.o2;
                }
                return super.getText(element);
            }
        };

        Font font = null;
        try {
            font = new Font(parentShell.getDisplay(), "Courier New", 10, SWT.NORMAL);
        } catch (Exception e) {
            Log.log(e);
        }
        final Font f = font;
        ListDialog d = new ListDialog(parentShell) {
            @Override
            protected Control createDialogArea(Composite container) {
                if (f != null) {
                    container.setFont(f);
                }
                return super.createDialogArea(container);
            }
        };
        IDialogSettings dialogSettings = SharedUiPlugin.getDefault().getDialogSettings();
        IDialogSettings section = dialogSettings
                .getSection("org.python.pydev.ui.dialogs.PyDialogHelpers.openCondaInterpreterSelection");
        if (section == null) {
            section = dialogSettings
                    .addNewSection("org.python.pydev.ui.dialogs.PyDialogHelpers.openCondaInterpreterSelection");
        }
        d.setDialogBoundsSettings(section, Dialog.DIALOG_PERSISTSIZE | Dialog.DIALOG_PERSISTLOCATION);
        try {
            d.setInput(nameAndExecutableList);
            d.setContentProvider(new ListContentProvider());
            d.setLabelProvider(labelProvider);
            d.setMessage(message);
            d.setTitle(title);
            d.open();
            if (d != null) {
                Object[] result = d.getResult();
                if (result != null && result.length == 1 && result[0] instanceof NameAndExecutable) {
                    return (NameAndExecutable) result[0];
                }
            }
        } finally {
            if (font != null && !font.isDisposed()) {
                font.dispose();
            }
        }
        return null;
    }

    private static List<NameAndExecutable> getAsNameAndExecutable(List<File> envs) {
        List<NameAndExecutable> ret = new ArrayList<NameAndExecutable>();
        if (PlatformUtils.isWindowsPlatform()) {
            for (File env : envs) {
                File exec = new File(env, "python.exe");
                if (FileUtils.enhancedIsFile(exec)) {
                    ret.add(new NameAndExecutable(env.getName(), exec.getPath()));
                } else {
                    Log.logInfo("Did not find: " + exec + " in conda environment.");
                }
            }
        } else {
            for (File env : envs) {
                File exec = new File(new File(env, "bin"), "python");
                if (FileUtils.enhancedIsFile(exec)) {
                    ret.add(new NameAndExecutable(env.getName(), exec.getPath()));
                } else {
                    Log.logInfo("Did not find: " + exec + " in conda environment.");
                }
            }
        }
        return ret;
    }

    /**
     * @param abstractInterpreterManager
     */
    public static boolean getAskAgainInterpreter(AbstractInterpreterManager m) {
        if (enableAskInterpreter < 0) {
            return false;
        }
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        return store.getBoolean(InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER + m.getInterpreterType());
    }

    public static void openException(String title, UnableToFindExecutableException e) {
        ErrorDialog.openError(UIUtils.getActiveShell(), title, e.getMessage(),
                new Status(IStatus.ERROR,
                        PydevPlugin.getPluginID(), e.getMessage(), e));
    }

    public static void showString(String string) {
        RunInUiThread.async(() -> {
            Display disp = Display.getCurrent();
            Shell shell = disp.getActiveShell();
            if (shell == null) {
                shell = new Shell(disp);
            }
            ShowTextDialog showTextDialog = new ShowTextDialog(shell, string);
            showTextDialog.open();
        });
    }

    private static final class ShowTextDialog extends Dialog {

        private String message;

        public ShowTextDialog(Shell shell, String message) {
            super(shell);
            this.message = message;
            setShellStyle(
                    SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MAX | getDefaultOrientation());
            setBlockOnOpen(true);
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Point getInitialSize() {
            return new Point(800, 600);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite composite = (Composite) super.createDialogArea(parent);

            GridLayout layout = (GridLayout) composite.getLayout();
            layout.numColumns = 1;
            createText(composite, message, 1);

            return composite;
        }

        private Text createText(Composite composite, String labelMsg, int colSpan) {
            Text text = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
            GridData gridData = new GridData(GridData.FILL_BOTH);
            gridData.horizontalSpan = colSpan;
            text.setLayoutData(gridData);
            text.setText(labelMsg);
            return text;
        }
    }
}
