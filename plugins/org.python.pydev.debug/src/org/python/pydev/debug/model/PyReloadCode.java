/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;

public class PyReloadCode implements IPyEditListener {

    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {

        //Reloading code removed for now (still too experimental)

        //        File file = edit.getEditorFile();
        //        if(file != null){
        //            IAdaptable context = DebugUITools.getDebugContext();
        //            if(context instanceof PyStackFrame){
        //                PyStackFrame stackFrame = (PyStackFrame) context;
        //                try{
        //                    IPythonNature pythonNature = edit.getPythonNature();
        //                    if(pythonNature != null){
        //                        String moduleName = pythonNature.resolveModule(file);
        //                        stackFrame.getTarget().postCommand(
        //                                new ReloadCodeCommand(stackFrame.getTarget(), moduleName));
        //                    }
        //                }catch(MisconfigurationException e){
        //                    PydevPlugin.log(e);
        //                }
        //                
        //            }
        //        }
    }

    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

}
