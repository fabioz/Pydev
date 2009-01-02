package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;

@SuppressWarnings("serial")
public class SystemASTManager extends AbstractASTManager{
    
    public SystemASTManager(IInterpreterManager manager, IPythonNature nature, IInterpreterInfo info) {
        this.modulesManager = info.getModulesManager();
        setNature(nature);
    }

    public void setProject(IProject project, IPythonNature nature, boolean restoreDeltas) {
        throw new RuntimeException("Not implemented");
    }

    public void rebuildModule(File file, IDocument doc, IProject project, IProgressMonitor monitor, IPythonNature nature) {
        throw new RuntimeException("Not implemented");
    }

    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

}
