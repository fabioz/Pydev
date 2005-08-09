/*
 * Created on 08/08/2005
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.ui.interpreters.IInterpreterManager;

public class PythonInterpreterEditor extends AbstractInterpreterEditor{

    public PythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.PYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        if (SimplePythonRunner.isWindowsPlatform()) {
            return new String[] { "*.exe", "*.*" };
        } 
        return null;
    }

}
