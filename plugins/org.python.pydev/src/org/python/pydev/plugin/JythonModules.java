package org.python.pydev.plugin;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.python.core.PyObject;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.shared_core.string.StringUtils;

public class JythonModules {

    private static final Object loadJythonLock = new Object();
    private static PyObject pep8Module;

    public static PyObject getPep8Module(IPythonInterpreter interpreter) {
        if (pep8Module == null) {
            pep8Module = getPepJythonModule(interpreter, "pycodestyle");
        }
        return pep8Module;
    }

    public static File getPep8Location() {
        return getPepModuleLocation("pycodestyle.py");
    }

    /**
     * @param module: The name of the module (i.e.: pycodestyle, autopep8)
     * @return null if it was not able to get the pycodestyle module.
     */
    private static PyObject getPepJythonModule(IPythonInterpreter interpreter, String module) {
        synchronized (loadJythonLock) {
            String s = ""
                    + "import sys\n"
                    + "add_to_pythonpath = '%s'\n"
                    + "if add_to_pythonpath not in sys.path:\n"
                    + "    sys.path.append(add_to_pythonpath)\n"
                    + "import " + module + "\n";
            //put the parent dir of pycodestyle.py in the pythonpath.
            File pepModuleLoc = getPepModuleLocation(module + ".py");
            if (pepModuleLoc == null) {
                return null;
            }
            s = StringUtils.format(s, StringUtils.replaceAllSlashes(pepModuleLoc.getParentFile().getAbsolutePath()));
            interpreter.exec(s);
            return interpreter.get(module);
        }
    }

    /**
     * @param moduleFilename: i.e.: pycodestyle.py, autopep8.py
     * @return
     */
    private static File getPepModuleLocation(String moduleFilename) {
        try {
            String pep8Location = PydevPlugin.getScriptWithinPySrc(
                    new Path("third_party").append("pep8").append(moduleFilename).toString()).toString();
            File pep8Loc = new File(pep8Location);
            if (!pep8Loc.exists()) {
                Log.log("Specified location for " + moduleFilename + " does not exist (" + pep8Location + ").");
                return null;
            }
            return pep8Loc;
        } catch (CoreException e) {
            Log.log("Error getting " + moduleFilename + " location", e);
            return null;
        }
    }

    private static ThreadLocal<IPythonInterpreter> iSortThreadLocalInterpreter = new ThreadLocal<>();

    public static String makeISort(String fileContents, File f) {
        IPythonInterpreter iPythonInterpreter = iSortThreadLocalInterpreter.get();
        IPythonInterpreter interpreter;
        if (iPythonInterpreter == null) {
            // The first call may be slow because doing the imports is slow, but subsequent calls should be
            // fast as we'll be reusing the same interpreter.
            String s = ""
                    + "import sys\n"
                    + "add_to_pythonpath = '%s'\n"
                    + "if add_to_pythonpath not in sys.path:\n"
                    + "    sys.path.append(add_to_pythonpath)\n"
                    + "import isort\n"
                    + "output = isort.SortImports(file_contents=fileContents, settings_path=settingsPath).output\n";

            boolean useConsole = false;

            interpreter = JythonPlugin.newPythonInterpreter(useConsole, false);
            String isortContainerLocation = null;
            try {
                isortContainerLocation = PydevPlugin.getScriptWithinPySrc(
                        new Path("third_party").append("isort_container").toString()).toString();
                File isortContainer = new File(isortContainerLocation);
                if (!isortContainer.exists()) {
                    Log.log("Specified location for isort_container does not exist (" + isortContainerLocation
                            + ").");
                    return null;
                }
            } catch (CoreException e) {
                Log.log("Error getting isort_container location", e);
                return null;
            }

            interpreter.set("fileContents", fileContents);
            if (f != null) {
                interpreter.set("settingsPath", f.getAbsoluteFile().getParent());
            } else {
                interpreter.set("settingsPath", "");
            }
            s = StringUtils.format(s, StringUtils.replaceAllSlashes(isortContainerLocation));
            interpreter.exec(s);
            iSortThreadLocalInterpreter.set(interpreter);
        } else {
            interpreter = iPythonInterpreter;
            // Found interpreter in thread local storage, just use it to do the sort.
            interpreter.set("fileContents", fileContents);
            if (f != null) {
                interpreter.set("settingsPath", f.getAbsoluteFile().getParent());
            } else {
                interpreter.set("settingsPath", "");
            }
            interpreter
                    .exec("output = isort.SortImports(file_contents=fileContents, settings_path=settingsPath).output");
        }

        PyObject pyObject = interpreter.get("output");
        if (pyObject != null) {
            return pyObject.toString();
        }
        return null;

    }
}
