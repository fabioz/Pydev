package org.python.pydev.core.package_manager;

public class UVPackageManager extends BasePackageManager {
    final public static String PREFS_NAME = "UV_BIN";
    private String pythonPath;

    public UVPackageManager(String projectRoot) {
        super(projectRoot, "uv", PREFS_NAME, "--directory");
    }

    /**
     * Returns the path to the python executable of a pyproject virtual environment.
     * 
     * @return the path to python
     */
    public String getPython() {
        if (pythonPath == null) {
            pythonPath = runCommand(getCommandArgs(new String[] { "python", "find" }));
        }
        return pythonPath;
    }
}
