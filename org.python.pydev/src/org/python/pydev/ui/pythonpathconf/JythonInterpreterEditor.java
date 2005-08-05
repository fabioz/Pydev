/*
 * License: Common Public License v1.0
 * Created on 04/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

public class JythonInterpreterEditor {

    public JythonInterpreterEditor() {
        super();
    }

    /**
     * true if executable is jython. A hack,
     */
    static public boolean isJython(String executable) {
        return executable.toLowerCase().indexOf("jython") != -1;
    }

}
