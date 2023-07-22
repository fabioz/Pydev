package org.python.pydev.jython;

import java.io.File;

import org.python.core.PyObject;
import org.python.pydev.core.CorePlugin;
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

}
