package org.python.pydev.jython;

import java.io.OutputStream;

import org.python.core.PyObject;

/**
 * This is a simple interface, just for exposing what we want from the PythonInterpreter
 */
public interface IPythonInterpreter {

	/**
	 * This method sets some variable in the interpreter
	 * 
	 * @param key the variable name
	 * @param value the variable value
	 */
	void set(String key, Object value);

	/**
	 * Executes a piece of code
	 * 
	 * @param exec The piece of code that should be executed
	 */
	void exec(String exec);

	/**
	 * This method returns the variable that we want to get from the interpreter as a java object
	 * 
	 * @param varName the variable that we want to get
	 * @param class_ the java class that should be used as the return value
	 * @return the object with the variable requested as a java object
	 */
	Object get(String varName, Class class_);

	/**
	 * This method returns the variable that we want to get from the interpreter as a PyObject
	 */
    PyObject get(String varName);

    /**
     * Cleans the interpreter
     */
	void cleanup();
	

    void setOut(OutputStream output);

    void setErr(OutputStream output);

}
