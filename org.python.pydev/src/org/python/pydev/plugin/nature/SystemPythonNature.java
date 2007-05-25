package org.python.pydev.plugin.nature;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.SystemASTManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class SystemPythonNature extends AbstractPythonNature implements IPythonNature{

	private IInterpreterManager manager;
	private SystemASTManager systemASTManager;

	public SystemPythonNature(IInterpreterManager manager){
		this.manager = manager;
	}
    
	public String getVersion() throws CoreException {
		throw new RuntimeException("Not Implemented");
	}

	public String getDefaultVersion() {
		throw new RuntimeException("Not Implemented");
	}

	public void setVersion(String version) throws CoreException {
		throw new RuntimeException("Not Implemented");
	}

	public boolean isJython() throws CoreException {
		return manager.isJython();
	}

	public boolean isPython() throws CoreException {
		return manager.isPython();
	}

	public boolean acceptsDecorators() throws CoreException {
		return true;
	}

	public int getRelatedId() throws CoreException {
		return PythonNature.getRelatedId(this);
	}

	public File getCompletionsCacheDir() {
		throw new RuntimeException("Not Implemented");
	}

	public void saveAstManager() {
		throw new RuntimeException("Not Implemented");
	}

	public IPythonPathNature getPythonPathNature() {
		throw new RuntimeException("Not Implemented");
	}

	public String resolveModule(String file) {
		InterpreterInfo info = (InterpreterInfo) this.manager.getDefaultInterpreterInfo(new NullProgressMonitor());
        if(info == null){
            return null;
        }
		return info.modulesManager.resolveModule(file);
	}

	public ICodeCompletionASTManager getAstManager() {
		if(systemASTManager == null){
			systemASTManager = new SystemASTManager(this.manager, this);
		}
		return systemASTManager;
	}

	public void configure() throws CoreException {
	}

	public void deconfigure() throws CoreException {
	}

	public IProject getProject() {
		return null;
	}

	public void setProject(IProject project) {
	}

	public void rebuildPath() {
		throw new RuntimeException("Not Implemented");
	}

	public void rebuildPath(String defaultSelectedInterpreter, IProgressMonitor monitor) {
		throw new RuntimeException("Not Implemented");
	}

    public IInterpreterManager getRelatedInterpreterManager() {
        return manager;
    }

	public IToken[] getBuiltinCompletions() {
		return null;
	}

	public void setBuiltinCompletions(IToken[] toks) {
	}

	public IModule getBuiltinMod() {
		return null;
	}

	public void setBuiltinMod(IModule mod) {
	}

    public int getGrammarVersion() {
        IInterpreterInfo info = manager.getDefaultInterpreterInfo(new NullProgressMonitor());
        if(info != null){
            return info.getGrammarVersion();
        }else{
            return IPythonNature.LATEST_GRAMMAR_VERSION;
        }
    }

}
