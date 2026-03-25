/**
 * Base package manager
 * 
 * Using either poetry or uv, manage the commands available to apply to a project.
 * 
 * @author Martin Whitehouse
 */
package org.python.pydev.core.package_manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.python.pydev.core.log.Log;

/**
 * Using ProcessBuilder, runs the commands to achieve e.g. `poetry install
 * package_name`.
 */
abstract public class BasePackageManager {
    public enum Commands {
        INSTALL("install"), ENV("env"), LOCK("lock"), SYNC("sync"), UPDATE("update");

        private String command;

        private Commands(String command) {
            this.command = command;
        }

        public String command() {
            return this.command;
        }
    }

    /**
     * Argument to use for changing to the project directory.
     */
    private String chDirArg;

    /**
     * Name of the task
     */
    protected String taskName;

    /**
     * Path to the project directory
     */
    private String projectRoot;

    /**
     * Path to the binary
     */
    private String binPath;

    /**
     * Title to use when binary can't be found.
     */
    private String errorTitle;
    /**
     * Message to display.
     */
    private String errorMsg;

    public BasePackageManager(String projectRoot, String name, String prefsName, String chDirAarg) {
        this.projectRoot = projectRoot;
        this.chDirArg = chDirAarg;
        binPath = Platform.getPreferencesService().getString("org.python.pydev", prefsName, null, null);
        File f = null;
        if (binPath != null) {
            f = new File(binPath);
        }
        if (binPath == null | !(f != null && f.exists() && !f.isDirectory())) {
            errorTitle = name + " not configured";
            errorMsg = "The path to " + name + "could not be found.\n";
            errorMsg += "Please configure the path to " + name;
        }
    }

    private Boolean checkError() {
        if (errorMsg != null) {
            Log.log(errorTitle + "\n" + errorMsg);
            return false;
        }
        return true;
    }

    protected ArrayList<String> getCommandArgs(String args) {
        return new ArrayList<String>(Arrays.asList(args));
    }

    protected ArrayList<String> getCommandArgs(String[] args) {
        ArrayList<String> parsedArgs = new ArrayList<String>();
        for (String arg : args) {
            parsedArgs.add(arg);
        }
        return parsedArgs;
    }

    public String add(String appName) {
        List<String> args = getCommandArgs("add");
        String[] apps = appName.split(" ");
        for (String app : apps) {
            args.add(app);
        }
        return runCommand(args);
    }

    public String lock() {
        return runCommand(getCommandArgs("lock"));
    }

    public String sync() {
        return runCommand(getCommandArgs("sync"));
    }

    public String remove(String appName) {
        List<String> args = getCommandArgs("remove");
        for (String app : appName.split(" ")) {
            args.add(app);
        }
        return runCommand(args);
    }

    /**
     * Returns the output of running `poetry args...`
     * 
     * @param args List of arguments to pass to binPath e.g. "[poetry] env info"
     * @return The command output
     */
    public String runCommand(List<String> args) {
        String output = null;
        if (!checkError()) {
            return output;
        }
        ArrayList<String> commandArgs = new ArrayList<String>();

        commandArgs.add(binPath);
        commandArgs.add(chDirArg);
        commandArgs.add(projectRoot);

        for (String arg : args) {
            commandArgs.add(arg);
        }

        ProcessBuilder pb = new ProcessBuilder(commandArgs);
        Process p;

        try {
            p = pb.start();
            output = new String(p.getInputStream().readAllBytes()).strip();
        } catch (IOException e) {
            Log.log(e);
        }

        return output;
    }

}
