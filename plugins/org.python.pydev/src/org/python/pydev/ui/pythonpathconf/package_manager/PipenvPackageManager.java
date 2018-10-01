package org.python.pydev.ui.pythonpathconf.package_manager;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.ast.codecompletion.shell.AbstractShell;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.runners.SimpleExeRunner;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.nature.PipenvHelper;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.process_window.ProcessWindow;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_ui.utils.UIUtils;
import org.python.pydev.ui.pythonpathconf.PipenvDialog;
import org.python.pydev.ui.pythonpathconf.ValidationFailedException;

public class PipenvPackageManager extends AbstractPackageManager {

    private final String pipenvLocation;
    private final String pipenvTargetDir;
    private final SystemPythonNature nature;

    public PipenvPackageManager(final InterpreterInfo interpreterInfo, IInterpreterManager interpreterManager)
            throws PipenvUnconfiguredException {
        super(interpreterInfo);

        String pipenvLocation = PipenvHelper.searchDefaultPipenvLocation(interpreterInfo, interpreterManager);
        String pipenvTargetDir = interpreterInfo.getPipenvTargetDir();
        boolean needsInfo = pipenvLocation == null || pipenvTargetDir == null;
        if (!needsInfo) {
            // Check that it's indeed compatible.
            if (checkPipenvInfoCompatible(pipenvLocation, pipenvTargetDir, interpreterInfo) != null) {
                needsInfo = true;
            }
        }
        if (needsInfo) {
            boolean showBaseInterpreter = false;
            PipenvDialog pipenvDialog = new PipenvDialog(UIUtils.getActiveShell(),
                    interpreterManager.getInterpreterInfos(),
                    pipenvLocation,
                    null, interpreterManager, "Pipenv information required for interpreter.", showBaseInterpreter) {

                @Override
                protected String getOkButtonText() {
                    return "Confirm pipenv information";
                }

                @Override
                protected void additionalValidation() throws ValidationFailedException {
                    String msg = checkPipenvInfoCompatible(this.getPipenvLocation(), this.getProjectLocation(),
                            interpreterInfo);
                    if (msg != null) {
                        setErrorMessage(msg);
                        throw new ValidationFailedException();
                    }
                    // Fix the interpreter info
                    interpreterInfo.setPipenvTargetDir(this.getProjectLocation());
                }
            };
            if (pipenvDialog.open() != PipenvDialog.OK) {
                throw new PipenvUnconfiguredException();
            }
        }
        this.pipenvLocation = pipenvLocation;
        this.pipenvTargetDir = interpreterInfo.getPipenvTargetDir();
        this.nature = new SystemPythonNature(interpreterManager, interpreterInfo);
    }

    private static String checkPipenvInfoCompatible(String pipenvLocation, String projectLocation,
            InterpreterInfo interpreterInfo) {
        // Check that the given location is valid for the given interpreter.
        File pythonPipenvFromLocation = PipenvHelper.getPythonPipenvFromLocation(pipenvLocation,
                new File(projectLocation));
        if (pythonPipenvFromLocation == null) {
            return "Unable to get the target python env from the location: " + projectLocation;
        }
        if (!pythonPipenvFromLocation.equals(new File(interpreterInfo.getExecutableOrJar()))) {
            return StringUtils.format(
                    "The target pipenv environment points to a different python.\nFound: %s\nExpected: %s",
                    pythonPipenvFromLocation, interpreterInfo.getExecutableOrJar());
        }
        return null;
    }

    @Override
    public List<String[]> list() {
        return null;
    }

    @Override
    protected String getPackageManagerName() {
        return "Pipenv";
    }

    @Override
    public void manage() {
        new PipenvProcessWindow(UIUtils.getActiveShell(), pipenvLocation, pipenvTargetDir, nature).open();
    }

    private static class PipenvProcessWindow extends ProcessWindow {

        private String pipenvLocation;
        private String projectLocation;
        private IPythonNature nature;

        public PipenvProcessWindow(Shell parentShell, final String pipenvLocation, final String projectLocation,
                final IPythonNature nature) {
            super(parentShell);
            this.pipenvLocation = pipenvLocation;
            this.projectLocation = projectLocation;
            this.nature = nature;

            this.setParameters(null, nature.getPythonPathNature(), new File(pipenvLocation), new File(projectLocation));
        }

        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText("Manage Pipenv");
        }

        @Override
        public Tuple<Process, String> createProcess(String[] arguments) {
            clearOutput();

            AbstractShell.restartAllShells();

            // note: not using the nature passed because we want to get the info from the system
            // so that pipenv can run properly and not from the configured python interpreter.
            final SimpleExeRunner simpleExeRunner = new SimpleExeRunner();
            return simpleExeRunner.run(
                    ArrayUtils.concatArrays(new String[] { pipenvLocation }, arguments),
                    new File(projectLocation),
                    null, new NullProgressMonitor());
        }

        @Override
        protected String[] getAvailableCommands() {
            return new String[] {
                    "install <package>",
                    "uninstall <package>",
                    "check",
                    "clean",
                    "graph",
                    "lock",
                    "sync",
                    "update",
            };
        }

        @Override
        protected String getSeeURL() {
            return "https://pipenv.readthedocs.io/";
        }
    }

    public static void create(final String executableOrJar, final String pipenvLocation, final String projectLocation,
            final SystemPythonNature nature) {
        PipenvProcessWindow processWindow = new PipenvProcessWindow(UIUtils.getActiveShell(), pipenvLocation,
                projectLocation, nature) {

            @Override
            protected void configureShell(Shell shell) {
                super.configureShell(shell);
                shell.setText("Create interpreter using Pipenv");
            }

            @Override
            protected String[] getAvailableCommands() {
                return ArrayUtils.concatArrays(new String[] {
                        "--python " + executableOrJar,
                }, super.getAvailableCommands());
            }

            @Override
            protected String getDescription() {
                return "It's possible to make additional calls to install other libraries with pipenv.\n"
                        + "Close dialog to proceed with the interpreter configuration.";
            }

        };
        processWindow.setAutoRun(true);
        processWindow.open();
    }
}
