package org.python.pydev.jython;

import org.python.core.PyObject;

public interface IPythonInterpreter {

	void set(String key, Object value);

	void exec(String exec);

	PyObject get(String string);
	
}
