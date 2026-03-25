package org.python.pydev.core.package_manager;

import java.util.ArrayList;

public class PoetryPackageManager extends BasePackageManager {
    final public static String PREFS_NAME = "POETRY_BIN";
    private String pythonPath;

    public PoetryPackageManager(String projectRoot) {
        super(projectRoot, "poetry", PREFS_NAME, "-C");
    }

    public String install() {
        return runCommand(getCommandArgs("install"));
    }

    @Override
    public String add(String appName) {
        ArrayList<String> args = getCommandArgs("add");
        String[] apps = appName.split(" ");
        for (String app : apps) {
            args.add(app);
        }
        return runCommand(args);
    }

    public String update() {
        return runCommand(getCommandArgs("update"));
    }

    /**
     * Returns the path to the python executable of a pyproject virtual environment.
     * 
     * @return the path to python
     */
    public String getPython() {
        if (pythonPath == null) {
            pythonPath = runCommand(getCommandArgs(new String[] { "env", "info", "-e" }));
        }
        return pythonPath;
    }
}
