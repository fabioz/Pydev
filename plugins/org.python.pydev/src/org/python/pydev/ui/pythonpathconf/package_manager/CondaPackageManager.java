package org.python.pydev.ui.pythonpathconf.package_manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.log.Log;
import org.python.pydev.process_window.ProcessWindow;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_ui.utils.UIUtils;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.ui.pythonpathconf.PythonInterpreterProviderFactory;

public class CondaPackageManager extends AbstractPackageManager {

    public CondaPackageManager(InterpreterInfo interpreterInfo) {
        super(interpreterInfo);
    }

    @Override
    public List<String[]> list() {
        List<String[]> listed = new ArrayList<String[]>();
        File condaExecutable;
        try {
            condaExecutable = findCondaExecutable();
        } catch (UnableToFindExecutableException e) {
            return errorToList(listed, e);
        }

        String executableOrJar = interpreterInfo.getExecutableOrJar();
        File prefix = new File(executableOrJar).getParentFile();

        String encoding = null; // use system encoding
        Tuple<String, String> output = new SimpleRunner().runAndGetOutput(
                new String[] { condaExecutable.toString(), "list", "-p", prefix.toString() }, null, null, null,
                encoding);

        List<String> splitInLines = StringUtils.splitInLines(output.o1, false);
        for (String line : splitInLines) {
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }
            List<String> split = StringUtils.split(line, ' ');
            if (split.size() == 3) {
                String p0 = split.get(0).trim();
                String p1 = split.get(1).trim();
                String p2 = split.get(2).trim();

                listed.add(new String[] { p0.trim(), p1.trim(), p2.trim() });
            }
        }
        return listed;
    }

    public File findCondaExecutable() throws UnableToFindExecutableException {
        File condaExecutable = null;
        try {
            condaExecutable = interpreterInfo.searchExecutableForInterpreter("conda", true);
        } catch (UnableToFindExecutableException e) {
            // Unable to find, let's see if it's in the path
            Set<String> pathsToSearch = PythonInterpreterProviderFactory.getPathsToSearch();
            List<File> searchedDirectories = new ArrayList<>();
            for (String string : pathsToSearch) {
                File file = InterpreterInfo.searchExecutableInContainer("conda", new File(string),
                        searchedDirectories);
                if (file != null) {
                    condaExecutable = file;
                }
            }
            if (condaExecutable == null) {
                throw e;
            }
        }
        return condaExecutable;
    }

    @Override
    protected String getPackageManagerName() {
        return "conda";
    }

    @Override
    public void manage() {
        final File condaExecutable;
        try {
            condaExecutable = findCondaExecutable();
        } catch (UnableToFindExecutableException e) {
            Log.log(e);
            PyDialogHelpers.openException("Unable to find conda", e);
            return;
        }
        ProcessWindow processWindow = new ProcessWindow(UIUtils.getActiveShell()) {

            @Override
            protected void configureShell(Shell shell) {
                super.configureShell(shell);
                shell.setText("Manage conda");
            }

            @Override
            protected String[] getAvailableCommands() {
                return new String[] {
                        "install -p " + new File(interpreterInfo.getExecutableOrJar()).getParent()
                };
            }

            @Override
            protected String getSeeURL() {
                return "https://conda.io/docs/commands.html";
            }

            @Override
            public Tuple<Process, String> createProcess(String[] arguments) {
                output.setText("");
                String[] cmdLine = ArrayUtils.concatArrays(new String[] { condaExecutable.toString() }, arguments);
                return new SimpleRunner().run(cmdLine, workingDir, null, null);
            }
        };
        processWindow.setParameters(null, null, condaExecutable, condaExecutable.getParentFile());
        processWindow.open();

    }
}
