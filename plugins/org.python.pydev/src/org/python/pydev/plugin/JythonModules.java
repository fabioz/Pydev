package org.python.pydev.plugin;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.python.core.PyObject;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.shared_core.string.StringUtils;

public class JythonModules {

    private static final Object loadJythonLock = new Object();
    private static PyObject pep8Module;

    public static PyObject getPep8Module(IPythonInterpreter interpreter) {
        if (pep8Module == null) {
            pep8Module = getPepJythonModule(interpreter, "pep8");
        }
        return pep8Module;
    }

    public static File getPep8Location() {
        return getPepModuleLocation("pep8.py");
    }

    /**
     * @param module: The name of the module (i.e.: pep8, autopep8)
     * @return null if it was not able to get the pep8 module.
     */
    private static PyObject getPepJythonModule(IPythonInterpreter interpreter, String module) {
        synchronized (loadJythonLock) {
            String s = ""
                    + "import sys\n"
                    + "add_to_pythonpath = '%s'\n"
                    + "if add_to_pythonpath not in sys.path:\n"
                    + "    sys.path.append(add_to_pythonpath)\n"
                    + "import " + module + "\n";
            //put the parent dir of pep8.py in the pythonpath.
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
     * @param moduleFilename: i.e.: pep8.py, autopep8.py
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
}
