package org.python.pydev.ui.pythonpathconf.package_manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.osgi.framework.Version;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.ast.runners.SimplePythonRunner;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.process_window.ProcessWindow;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_ui.utils.UIUtils;

public class PipPackageManager extends AbstractPackageManager {

    public PipPackageManager(InterpreterInfo interpreterInfo) {
        super(interpreterInfo);
    }

    /**
     * To be called from any thread
     */
    @Override
    public List<String[]> list() {
        List<String[]> listed = new ArrayList<String[]>();
        File pipExecutable;
        Tuple<String, String> output;
        try {
            pipExecutable = interpreterInfo.searchExecutableForInterpreter("pip", false);
            String encoding = null; // use system encoding
            output = new SimpleRunner().runAndGetOutput(
                    new String[] { pipExecutable.toString(), "list", "--format=columns" }, null, null, null,
                    encoding);
        } catch (UnableToFindExecutableException e) {
            IPythonNature nature = new SystemPythonNature(interpreterInfo.getModulesManager().getInterpreterManager(),
                    interpreterInfo);
            String[] parameters = SimplePythonRunner.preparePythonCallParameters(
                    interpreterInfo.executableOrJar, "-m",
                    new String[] { getPipModuleName(interpreterInfo), "list", "--format=columns" });

            output = new SimplePythonRunner().runAndGetOutput(
                    parameters, null, nature, null, "utf-8");
        }

        List<String> splitInLines = StringUtils.splitInLines(output.o1, false);
        for (String line : splitInLines) {
            line = line.trim();
            List<String> split = StringUtils.split(line, ' ');
            if (split.size() == 2) {
                String p0 = split.get(0).trim();
                String p1 = split.get(1).trim();

                if (p0.toLowerCase().equals("package")
                        && p1.toLowerCase().equals("version")) {
                    continue;
                }
                if (p0.toLowerCase().startsWith("--")
                        && p1.toLowerCase().startsWith("--")) {
                    continue;
                }
                listed.add(new String[] { p0.trim(), p1.trim(), "<pip>" });
            }
        }
        if (output.o2.toLowerCase().contains("no module named pip")) {
            listed.add(new String[] { "pip not installed (or not found) in interpreter", "", "" });
        } else {
            for (String s : StringUtils.iterLines(output.o2)) {
                listed.add(new String[] { s, "", "" });
            }
        }
        return listed;
    }

    private static String getPipModuleName(InterpreterInfo interpreterInfo) {
        String version = interpreterInfo.getVersion();
        Version version2 = new Version(version);
        if (version2.getMajor() <= 2 && version2.getMinor() <= 6) {
            return "pip.__main__";
        }
        return "pip";
    }

    @Override
    protected String getPackageManagerName() {
        return "pip";
    }

    @Override
    public void manage() {
        File pipExecutable;
        String[] availableCommands = new String[] {
                "install <package>",
                "uninstall <package>",
        };
        try {
            pipExecutable = interpreterInfo.searchExecutableForInterpreter("pip", false);
        } catch (UnableToFindExecutableException e) {
            availableCommands = new String[] {
                    "-m " + getPipModuleName(interpreterInfo) + " install <package>",
                    "-m " + getPipModuleName(interpreterInfo) + " uninstall <package>",
            };
            pipExecutable = new File(interpreterInfo.getExecutableOrJar());
        }
        final String[] availableCommandsFinal = availableCommands;
        final File pipExecutableFinal = pipExecutable;

        ProcessWindow processWindow = new ProcessWindow(UIUtils.getActiveShell()) {

            @Override
            protected void configureShell(Shell shell) {
                super.configureShell(shell);
                shell.setText("Manage pip");
            }

            @Override
            protected String[] getAvailableCommands() {
                return availableCommandsFinal;
            }

            @Override
            protected String getSeeURL() {
                return "https://pip.pypa.io/en/stable";
            }

            @Override
            public Tuple<Process, String> createProcess(String[] arguments) {
                clearOutput();
                String[] cmdLine = ArrayUtils.concatArrays(new String[] { pipExecutableFinal.toString() }, arguments);
                return new SimpleRunner().run(cmdLine, workingDir, null, null);
            }
        };
        processWindow.setParameters(null, null, pipExecutableFinal, pipExecutableFinal.getParentFile());
        processWindow.open();

    }
}
