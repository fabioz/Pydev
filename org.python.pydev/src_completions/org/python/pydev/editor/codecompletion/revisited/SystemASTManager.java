package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

@SuppressWarnings("serial")
public class SystemASTManager extends AbstractASTManager{
    
    private IInterpreterManager manager;

    public SystemASTManager(IInterpreterManager manager, IPythonNature nature) {
        this.manager = manager;
        InterpreterInfo info = (InterpreterInfo) this.manager.getDefaultInterpreterInfo(new NullProgressMonitor());
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

    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor, String defaultSelectedInterpreter) {
        throw new RuntimeException("Not implemented");
    }

}
