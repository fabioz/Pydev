package org.python.pydev.plugin.nature;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;


public class PyNatureReindexer implements IPyEditListener{

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
        edit.addOfflineActionListener("--reindex", new Action(){
            @Override
            public void run() {
                for (IPythonNature nature : PythonNature.getAllPythonNatures()) {
                    nature.rebuildPath();
                }
            }
        }, "Rebuilds the internal structure for all Pydev projects.", true);
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
    }

}
