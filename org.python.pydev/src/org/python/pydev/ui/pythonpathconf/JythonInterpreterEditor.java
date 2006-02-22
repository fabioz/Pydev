/*
 * License: Common Public License v1.0
 * Created on 04/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;

public class JythonInterpreterEditor extends AbstractInterpreterEditor{

    public JythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.JYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        return new String[] { "*.jar", "*.*" };
    }

}