/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;


public class PyRunToLineTarget implements IRunToLineTarget {

    @Override
    public boolean canRunToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target) {
        return true;
    }

    @Override
    public void runToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target) throws CoreException {
        //System.out.println("Run to line:"+target);
        PyStackFrame stack = null;
        if (target instanceof PyStackFrame) {
            stack = (PyStackFrame) target;
            target = stack.getThread();
        }

        if (!(part instanceof PyEdit)) {
            return;
        }

        PyEdit pyEdit = (PyEdit) part;
        SimpleNode ast = pyEdit.getAST();
        if (ast == null) {
            IDocument doc = pyEdit.getDocument();
            SourceModule sourceModule;
            IPythonNature nature = null;
            try {
                nature = pyEdit.getPythonNature();
            } catch (MisconfigurationException e) {
                //Let's try to find a suitable nature
                File editorFile = pyEdit.getEditorFile();
                if (editorFile == null || !editorFile.exists()) {
                    Log.log(e);
                    return;
                }
                nature = PydevPlugin.getInfoForFile(editorFile).o1;
            }

            if (nature == null) {
                Log.log("Unable to determine nature!");
                return;
            }

            try {
                sourceModule = (SourceModule) AbstractModule.createModuleFromDoc("", null, doc, nature, true);
            } catch (MisconfigurationException e) {
                Log.log(e);
                return;
            }
            ast = sourceModule.getAst();
        }

        if (ast == null) {
            Log.log("Cannot determine context to run to.");
            return;
        }

        if (target instanceof PyThread && selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection) selection;
            PyThread pyThread = (PyThread) target;
            if (!pyThread.isPydevThread()) {
                int line = textSelection.getStartLine();
                String functionName = NodeUtils.getContextName(line, ast);
                if (functionName == null) {
                    functionName = ""; //global context
                } else {
                    functionName = FullRepIterable.getLastPart(functionName).trim();
                }
                pyThread.runToLine(line + 1, functionName);
                return;
            }
        }
    }
}
