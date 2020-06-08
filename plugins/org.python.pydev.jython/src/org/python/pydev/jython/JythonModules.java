package org.python.pydev.jython;

import java.io.File;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.jython.IPythonInterpreter;
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
            File pepModuleLoc = CorePlugin.getPepModuleLocation(module + ".py");
            if (pepModuleLoc == null) {
                return null;
            }
            s = StringUtils.format(s, StringUtils.replaceAllSlashes(pepModuleLoc.getParentFile().getAbsolutePath()));
            interpreter.exec(s);
            return (PyObject) interpreter.get(module);
        }
    }

    private static ThreadLocal<IPythonInterpreter> iSortThreadLocalInterpreter = new ThreadLocal<>();

    public static String makeISort(String fileContents, File f, Set<String> knownThirdParty) {
        IPythonInterpreter iPythonInterpreter = iSortThreadLocalInterpreter.get();
        IPythonInterpreter interpreter;
        String outputLine = "output = getattr(isort.SortImports(file_contents=fileContents, settings_path=settingsPath, known_third_party=knownThirdParty), 'output', None)\n";
        if (iPythonInterpreter == null) {
            // The first call may be slow because doing the imports is slow, but subsequent calls should be
            // fast as we'll be reusing the same interpreter.
            String s = ""
                    + "import sys\n"
                    + "import os\n"
                    + "add_to_pythonpath = '%s'\n"
                    + "os.chdir(add_to_pythonpath)\n"
                    + "if add_to_pythonpath not in sys.path:\n"
                    + "    sys.path.append(add_to_pythonpath)\n"
                    + "import isort\n"
                    + outputLine;

            boolean useConsole = false;

            interpreter = JythonPlugin.newPythonInterpreter(useConsole, false);
            String isortContainerLocation = null;
            try {
                isortContainerLocation = CorePlugin.getScriptWithinPySrc(
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
            interpreter.set("knownThirdParty", new PyList(knownThirdParty));
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
            interpreter.set("knowhThirdParty", new PyList(knownThirdParty));
            // Note that we have to clear the global caches that isort has for it to reload the settings (otherwise,
            // eclipse needs to be restarted just to get the updated caches).
            interpreter
                    .exec(""
                            + "isort.settings._get_config_data.cache_clear()\n"
                            + "isort.settings.from_path.cache_clear()\n"
                            + outputLine);
        }

        PyObject pyObject = (PyObject) interpreter.get("output");
        if (pyObject != null && pyObject.__nonzero__()) {
            return pyObject.toString();
        }
        return null;

    }
}
