/*
 * Created on Mar 19, 2006
 */
package org.python.pydev.editor.scripting;

import java.util.HashMap;
import java.util.ListResourceBundle;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;

/**
 * This class is used for scripting in Pydev.
 * It listens to the PyEdit actions and passes what is needed for the interpreter.
 * 
 * @author Fabio
 */
public class PyEditScripting implements IPyEditListener {

    private IPythonInterpreter interpreter;

    public PyEditScripting(){
        interpreter = JythonPlugin.newPythonInterpreter();
    }
    
    public void onSave(PyEdit edit) {
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit) {
        // I was going to do some things in jython here, but there is too much code around for that...
        HashMap<String, Object> locals = new HashMap<String, Object>();
        locals.put("cmd", "onCreateActions");
        locals.put("editor", edit);
        JythonPlugin.exec(locals, "pyedit.py", interpreter);
    }

    public void onDispose(PyEdit edit) {
        interpreter = null;
    }

    public void onSetDocument(IDocument document, PyEdit edit) {
    }

}
