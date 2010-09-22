package org.python.pydev.jython;

import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class PythonInterpreterWrapperNotShared extends PythonInterpreter implements IPythonInterpreter{
    
    public PythonInterpreterWrapperNotShared() {
        super(null, createPySystemState());
    }

    public static PySystemState createPySystemState() {
        try {
            return new PySystemState();
        } catch (IllegalStateException e) {
            //happens when running tests.
            PySystemState.initialize();
            return new PySystemState();
        }
    }
    

}
