package org.python.pydev.ui.pythonpathconf.package_manager;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.runners.SimpleExeRunner;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.process_window.ProcessWindow;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_ui.utils.UIUtils;

public class PipenvPackageManager extends AbstractPackageManager {

    public PipenvPackageManager(InterpreterInfo interpreterInfo) {
        super(interpreterInfo);
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

    }

    public static void create(final String executableOrJar, final String pipenvLocation, final String projectLocation,
            final SimpleExeRunner simpleExeRunner, final SystemPythonNature nature) {
        ProcessWindow processWindow = new ProcessWindow(UIUtils.getActiveShell()) {

            @Override
            protected void configureShell(Shell shell) {
                super.configureShell(shell);
                shell.setText("Create pipenv");
            }

            @Override
            protected String[] getAvailableCommands() {
                return new String[] {
                        "--python " + executableOrJar,
                        "install <package>",
                        "uninstall <package>",
                };
            }

            @Override
            protected String getSeeURL() {
                return null;
            }

            @Override
            public Tuple<Process, String> createProcess(String[] arguments) {
                clearOutput();
                return simpleExeRunner.run(
                        ArrayUtils.concatArrays(new String[] { pipenvLocation }, arguments),
                        new File(projectLocation),
                        nature, new NullProgressMonitor());
            }
        };
        IContainer container = null;
        IPythonPathNature pythonPathNature = nature.getPythonPathNature();
        File targetExecutable = new File(pipenvLocation);
        File workingDir = new File(projectLocation);
        processWindow.setParameters(container, pythonPathNature, targetExecutable, workingDir);
        processWindow.setAutoRun(true);
        processWindow.open();
    }
}
