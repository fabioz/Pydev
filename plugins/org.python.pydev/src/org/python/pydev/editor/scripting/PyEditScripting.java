/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 19, 2006
 */
package org.python.pydev.editor.scripting;

import java.util.HashMap;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.jython.IPythonInterpreter;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;

/**
 * This class is used for scripting in Pydev.
 * It listens to the PyEdit actions and passes what is needed for the interpreter.
 * 
 * A new PyEditScripting is created for each editor. Therefore, we have one interpreter for each editor.
 * 
 * @author Fabio
 */
public class PyEditScripting implements IPyEditListener {

    private IPythonInterpreter interpreter;

    private static Object systemGlobals = null;

    public PyEditScripting() {
        createInterpreter();
    }

    private void createInterpreter() {
        if (interpreter == null) {
            interpreter = JythonPlugin.newPythonInterpreter();
            if (systemGlobals == null) {
                interpreter.exec("systemGlobals = {}");
                systemGlobals = interpreter.get("systemGlobals");
            }
        }
    }

    private void doExec(HashMap<String, Object> locals) {
        createInterpreter();
        locals.put("systemGlobals", systemGlobals);
        JythonPlugin.execAll(locals, "pyedit", interpreter); //execute all the files that start with 'pyedit' that are located beneath
                                                             //the org.python.pydev.jython/jysrc directory and some user specified dir (if any).
    }

    public void onSave(BaseEditor edit, IProgressMonitor monitor) {
        HashMap<String, Object> locals = new HashMap<String, Object>();
        locals.put("cmd", "onSave");
        locals.put("editor", edit);
        doExec(locals);
    }

    public void onCreateActions(ListResourceBundle resources, BaseEditor edit, IProgressMonitor monitor) {
        HashMap<String, Object> locals = new HashMap<String, Object>();
        locals.put("cmd", "onCreateActions");
        locals.put("editor", edit);
        doExec(locals);
    }

    public void onDispose(BaseEditor edit, IProgressMonitor monitor) {
        HashMap<String, Object> locals = new HashMap<String, Object>();
        locals.put("cmd", "onDispose");
        locals.put("editor", edit);
        doExec(locals);

        interpreter.cleanup();
        interpreter = null;
    }

    public void onSetDocument(IDocument document, BaseEditor edit, IProgressMonitor monitor) {
        HashMap<String, Object> locals = new HashMap<String, Object>();
        locals.put("cmd", "onSetDocument");
        locals.put("document", document);
        locals.put("editor", edit);
        doExec(locals);
    }

}
