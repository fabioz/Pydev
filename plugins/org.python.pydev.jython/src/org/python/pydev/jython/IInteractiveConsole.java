/*
 * Created on Mar 21, 2006
 */
package org.python.pydev.jython;


public interface IInteractiveConsole extends IPythonInterpreter{

    boolean push(String input);

}
