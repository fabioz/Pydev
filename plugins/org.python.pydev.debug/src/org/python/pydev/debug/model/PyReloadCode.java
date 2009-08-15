package org.python.pydev.debug.model;

import java.io.File;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.debug.model.remote.ReloadCodeCommand;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;

public class PyReloadCode implements IPyEditListener{

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor){
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor){
    }

    public void onSave(PyEdit edit, IProgressMonitor monitor){
        
        File file = edit.getEditorFile();
        if(file != null){
            IAdaptable context = DebugUITools.getDebugContext();
            if(context instanceof PyStackFrame){
                PyStackFrame stackFrame = (PyStackFrame) context;
                try{
                    String moduleName = edit.getPythonNature().resolveModule(file);
                    stackFrame.getTarget().postCommand(
                            new ReloadCodeCommand(stackFrame.getTarget(), moduleName));
                }catch(MisconfigurationException e){
                    PydevPlugin.log(e);
                }
                
            }
        }
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor){
    }

}
