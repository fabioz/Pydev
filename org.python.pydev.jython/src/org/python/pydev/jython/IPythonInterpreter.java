package org.python.pydev.jython;

import org.python.core.PyObject;


public interface IPythonInterpreter {

	void set(String key, Object value);

	void exec(String exec);

	Object get(String string, Class class_);

    PyObject get(String string);
	
}
