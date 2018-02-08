package org.python.pydev.jython;

import java.util.List;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.pydev.shared_core.jython.IPythonInterpreter;
import org.python.pydev.shared_core.jython.JythonPep8Core;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class JythonPep8 {

    public static final String EXECUTE_PEP8 = "import sys\n"
            + "argv = ['pycodestyle.py', r'%s'%s]\n"
            + "sys.argv=argv\n"
            //It always accesses sys.argv[0] in process_options, so, it must be set.
            + "\n"
            + "\n"
            + "pep8style = pycodestyle.StyleGuide(parse_argv=True, config_file=False)\n"
            + "\n"
            + "checker = pycodestyle.Checker(options=pep8style.options, filename='%s', lines=lines)\n"
            + "\n"
            + "if ReportError is None: #Only redefine if it wasn't defined already\n"
            + "    class ReportError:\n"
            + "\n"
            + "        def __init__(self, checker, pep8style, visitor):\n"
            + "            self.checker = checker\n"
            + "            self.pep8style = pep8style\n"
            + "            self.visitor = visitor\n"
            + "            self.original = checker.report_error\n"
            + "            checker.report_error = self\n"
            + "            if not self.pep8style.excluded(self.checker.filename):\n"
            + "                checker.check_all()\n"
            + "            #Clear references\n"
            + "            self.original = None\n"
            + "            self.checker = None\n"
            + "            self.pep8style = None\n"
            + "            self.visitor = None\n"
            + "            checker.report_error = None\n"
            + "        \n"
            + "        def __call__(self, line_number, offset, text, check):\n"
            + "            code = text[:4]\n"
            + "            if self.pep8style.options.ignore_code(code):\n"
            + "                return\n"
            + "            self.visitor.reportError(line_number, offset, text, check)\n"
            + "            return self.original(line_number, offset, text, check)\n"
            + "\n"
            + "ReportError(checker, pep8style, visitor)\n"
            + "checker = None #Release checker\n"
            + "pep8style = None #Release pep8style\n"
            + "";

    public static final Object lock = new Object();

    public volatile static PyObject reportError;

    public static void analyzePep8WithJython(JythonPep8Core pep8Params) {
        FastStringBuffer args = new FastStringBuffer(pep8Params.pep8CommandLine.length * 20);
        for (String string : pep8Params.pep8CommandLine) {
            args.append(',').append("r'").append(string).append('\'');
        }

        //It's important that the interpreter is created in the Thread and not outside the thread (otherwise
        //it may be that the output ends up being shared, which is not what we want.)
        IPythonInterpreter interpreter = JythonPlugin.newPythonInterpreter(pep8Params.useConsole, false);
        String file = StringUtils.replaceAllSlashes(pep8Params.absolutePath);
        interpreter.set("visitor", pep8Params.visitor);

        List<String> splitInLines = StringUtils.splitInLines(pep8Params.document.get());
        interpreter.set("lines", splitInLines);
        PyObject tempReportError = reportError;
        if (tempReportError != null) {
            interpreter.set("ReportError", tempReportError);
        } else {
            interpreter.set("ReportError", Py.None);
        }
        PyObject pep8Module = JythonModules.getPep8Module(interpreter);
        interpreter.set("pycodestyle", pep8Module);

        String formatted = StringUtils.format(EXECUTE_PEP8, file,
                args.toString(), file);
        interpreter.exec(formatted);
        if (reportError == null) {
            synchronized (lock) {
                if (reportError == null) {
                    reportError = (PyObject) interpreter.get("ReportError");
                }
            }
        }
    }

}
