/*
 * License: Common Public License v1.0
 * Created on 04/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.ui.IInterpreterManager;

public class JythonInterpreterEditor extends InterpreterEditor{

    public JythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(labelText, parent, interpreterManager);
    }

    /**
     * true if executable is jython. A hack,
     */
    static public boolean isJython(String executable) {
        return executable.toLowerCase().indexOf("jython") != -1;
    }

}