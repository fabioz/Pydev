package org.python.pydev.plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
    private static Map<String, PyObject> autopep8Modules;

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

    public static IPythonInterpreter createInterpreterWithAutopep8Requisites() {
        String[] MODULES = new String[] {
                "autopep8",
                "lib2to3",

                "lib2to3.btm_matcher",
                "lib2to3.btm_utils",
                "lib2to3.fixer_base",
                "lib2to3.fixer_util",
                "lib2to3.main",
                "lib2to3.patcomp",
                "lib2to3.pygram",
                "lib2to3.pytree",
                "lib2to3.refactor",

                "lib2to3.pgen2",
                "lib2to3.pgen2.driver",
                "lib2to3.pgen2.grammar",
                "lib2to3.pgen2.literals",
                "lib2to3.pgen2.parse",
                "lib2to3.pgen2.pgen",
                "lib2to3.pgen2.token",
                "lib2to3.pgen2.tokenize",

                "lib2to3.fixes.fix_ws_comma",
                "lib2to3.fixes.fix_idioms",
                "lib2to3.fixes.fix_has_key",
                "lib2to3.fixes.fix_ne",
                "lib2to3.fixes.fix_repr",
                "lib2to3.fixes.fix_apply",
                "lib2to3.fixes.fix_except",
                "lib2to3.fixes.fix_exitfunc",
                "lib2to3.fixes.fix_import",
                "lib2to3.fixes.fix_numliterals",
                "lib2to3.fixes.fix_operator",
                "lib2to3.fixes.fix_paren",
                "lib2to3.fixes.fix_reduce",
                "lib2to3.fixes.fix_renames",
                "lib2to3.fixes.fix_standarderror",
                "lib2to3.fixes.fix_sys_exc",
                "lib2to3.fixes.fix_throw",
                "lib2to3.fixes.fix_tuple_params",
                "lib2to3.fixes.fix_xreadlines",
                "lib2to3.fixes.fix_raise",
        };

        IPythonInterpreter interpreter = JythonPlugin.newPythonInterpreter(false, false);
        PyObject pep8Module = getPep8Module(interpreter);
        interpreter.set("pep8", pep8Module);
        interpreter.exec(""
                + "import sys\n"
                + "sys.modules['pep8']=pep8\n");

        File pep8Dir;
        try {
            pep8Dir = PydevPlugin.getScriptWithinPySrc(new Path("third_party").append("pep8").toString());
        } catch (CoreException e) {
            Log.log(e);
            return null;
        }
        interpreter.exec(StringUtils.format(""
                + "add_to_pythonpath = '%s'\n"
                + "if add_to_pythonpath not in sys.path:\n"
                + "    sys.path.append(add_to_pythonpath)\n"
                + "\n", StringUtils.replaceAllSlashes(pep8Dir.getAbsolutePath())));

        if (autopep8Modules == null) {
            synchronized (loadJythonLock) {

                String s = "";
                for (String mod : MODULES) {
                    s += "import " + mod + " as " + mod.replace('.', '_') + "\n";
                }
                interpreter.exec(s);

                Map<String, PyObject> m = new HashMap<>();

                for (String mod : MODULES) {
                    m.put(mod.replace('.', '_'), interpreter.get(mod.replace('.', '_')));
                }
                autopep8Modules = m;
            }
        }

        for (String mod : MODULES) {
            interpreter.set(mod.replace('.', '_'), autopep8Modules.get(mod.replace('.', '_')));
            interpreter.exec("sys.modules['" + mod + "']=" + mod.replace('.', '_'));
        }
        return interpreter;
    };

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
