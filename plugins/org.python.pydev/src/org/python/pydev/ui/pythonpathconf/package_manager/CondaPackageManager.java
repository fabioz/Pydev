package org.python.pydev.ui.pythonpathconf.package_manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Shell;
import org.python.pydev.ast.codecompletion.shell.AbstractShell;
import org.python.pydev.ast.interpreter_managers.PyDevCondaPreferences;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.log.Log;
import org.python.pydev.json.eclipsesource.JsonArray;
import org.python.pydev.json.eclipsesource.JsonObject;
import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.process_window.ProcessWindow;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.shared_ui.utils.UIUtils;
import org.python.pydev.ui.dialogs.PyDialogHelpers;

public class CondaPackageManager extends AbstractPackageManager {

    private File prefix;

    public CondaPackageManager(IInterpreterInfo interpreterInfo, File prefix) {
        super(interpreterInfo);
        this.prefix = prefix;
    }

    public static List<File> listCondaEnvironments(File condaExecutable) {
        String encoding = "utf-8";
        Tuple<String, String> output = new SimpleRunner().runAndGetOutput(
                new String[] { condaExecutable.toString(), "env", "list", "--json" }, null, null,
                null,
                encoding);
        Log.logInfo(output.o1);
        JsonObject jsonOutput = JsonValue.readFrom(output.o1).asObject();
        JsonArray envs = jsonOutput.get("envs").asArray();
        Set<File> set = new HashSet<>();
        for (JsonValue env : envs.values()) {
            set.add(new File(env.asString()));
        }
        return new ArrayList<File>(set);
    }

    @Override
    public List<String[]> list() {
        List<String[]> listed = new ArrayList<String[]>();
        File condaExecutable;
        try {
            condaExecutable = PyDevCondaPreferences.findCondaExecutable(interpreterInfo);
        } catch (UnableToFindExecutableException e) {
            return errorToList(listed, e);
        }

        String encoding = null; // use system encoding
        Tuple<String, String> output = new SimpleRunner().runAndGetOutput(
                new String[] { condaExecutable.toString(), "list", "-p", prefix.toString(), "--json" }, null, null,
                null,
                encoding);

        try {
            JsonValue readFrom = JsonValue.readFrom(output.o1);
            JsonArray asArray = readFrom.asArray();
            for (JsonValue value : asArray) {
                JsonObject asObject = value.asObject();
                JsonValue name = asObject.get("name");
                JsonValue version = asObject.get("version");
                JsonValue channel = asObject.get("channel");
                JsonValue buildString = asObject.get("build_string");
                listed.add(new String[] { name.asString(), version.asString(),
                        StringUtils.join("", buildString.asString(), " (", channel.asString(), ")") });
            }
        } catch (Exception e) {
            // Older version of Conda had a different json format and "list --json" wouldn't show pip info, so,
            // fallback to an implementation which just did a conda list without --json and parse its output
            output = new SimpleRunner().runAndGetOutput(
                    new String[] { condaExecutable.toString(), "list", "-p", prefix.toString() }, null, null,
                    null,
                    encoding);
            List<String> splitInLines = StringUtils.splitInLines(output.o1, false);
            for (String line : splitInLines) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                List<String> split = StringUtils.split(line, ' ');
                if (split.size() >= 3) {
                    listed.add(new String[] { split.get(0), split.get(1),
                            StringUtils.join(" - ", split.subList(2, split.size() - 1)) });
                } else if (split.size() == 2) {
                    listed.add(new String[] { split.get(0), split.get(1), "" });
                }
            }
        }
        return listed;
    }

    @Override
    protected String getPackageManagerName() {
        return "conda";
    }

    @Override
    public void manage() {
        manage(new String[0], false, null);
    }

    public void manage(String[] initialCommands, boolean autoRun, File workingDir) {
        final File condaExecutable;
        try {
            condaExecutable = PyDevCondaPreferences.findCondaExecutable(interpreterInfo);
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
                List<String> lst = new ArrayList<>(Arrays.asList(initialCommands));
                final String prefixDir = new File(interpreterInfo.getExecutableOrJar()).getParent();
                String prefixInfo = " -p " + prefixDir;
                for (int i = 0; i < lst.size(); i++) {
                    String existing = lst.get(i);
                    if (!existing.contains("-p")) {
                        existing = existing.trim();
                        if (existing.startsWith("install") || existing.startsWith("uninstall")
                                || existing.startsWith("upgrade") || existing.startsWith("update")
                                || existing.startsWith("clean") || existing.startsWith("list")
                                || existing.startsWith("package") || existing.startsWith("remove")
                                || existing.startsWith("search")) {
                            existing += prefixInfo;
                        }
                        lst.set(i, existing);
                    }
                }
                return ArrayUtils.concatArrays(lst.toArray(new String[0]), new String[] {
                        "install -p " + prefixDir + " <package>",
                        "uninstall -p " + prefixDir + " <package>"
                });
            }

            @Override
            protected String getSeeURL() {
                return "https://conda.io/docs/commands.html";
            }

            @Override
            public Tuple<Process, String> createProcess(String[] arguments) {
                clearOutput();

                AbstractShell.restartAllShells();

                String[] cmdLine = ArrayUtils.concatArrays(new String[] { condaExecutable.toString() }, arguments);
                return new SimpleRunner().run(cmdLine, workingDir, null, null);
            }
        };
        if (workingDir == null) {
            workingDir = condaExecutable.getParentFile();
        }
        processWindow.setParameters(null, null, condaExecutable, workingDir);
        processWindow.setAutoRun(autoRun);
        processWindow.open();
    }

}
