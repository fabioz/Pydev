/*
 * Created on 08/08/2005
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;

public class PythonInterpreterEditor extends AbstractInterpreterEditor{

    public PythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.PYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        if (REF.isWindowsPlatform()) {
            return new String[] { "*.exe", "*.*" };
        } 
        return null;
    }

    
    protected String getAutoNewInput() {
        return "python"; //This should be enough to find it from the PATH or any other way it's defined.
    }
    
    
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        this.autoConfigButton.setToolTipText("Will try to find Python on the PATH (will fail if not available)");
    }
}
