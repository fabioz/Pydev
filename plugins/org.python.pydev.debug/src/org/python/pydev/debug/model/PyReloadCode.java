package org.python.pydev.debug.model;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;

public class PyReloadCode implements IPyEditListener{

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor){
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor){
    }

    public void onSave(PyEdit edit, IProgressMonitor monitor){
        
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

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor){
    }

}
