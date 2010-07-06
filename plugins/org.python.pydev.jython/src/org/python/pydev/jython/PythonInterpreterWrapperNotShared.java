package org.python.pydev.jython;

import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class PythonInterpreterWrapperNotShared extends PythonInterpreter implements IPythonInterpreter{
    
    public PythonInterpreterWrapperNotShared() {
        super(null, new PySystemState());
    }
    

}
