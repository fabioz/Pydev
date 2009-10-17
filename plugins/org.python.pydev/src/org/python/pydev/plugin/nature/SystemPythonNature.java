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
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.revisited.SystemASTManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * This nature is used only as a 'last resort', if we're unable to link a given resource to
 * a project (and thus, we don't have project-related completions and we don't know with what
 * exactly we're dealing with: it's usually only used for external files)
 */
public class SystemPythonNature extends AbstractPythonNature implements IPythonNature{

    private final IInterpreterManager manager;
    public final IInterpreterInfo info;
    private SystemASTManager systemASTManager;

    public SystemPythonNature(IInterpreterManager manager) throws MisconfigurationException{
        this(manager, manager.getDefaultInterpreterInfo(new NullProgressMonitor()));
    }
    
    public SystemPythonNature(IInterpreterManager manager, IInterpreterInfo info){
        this.info = info;
        this.manager = manager;
    }
    
    public String getVersion() throws CoreException {
        if(this.info != null){
            String version = this.info.getVersion();
            if(version != null && version.startsWith("3")){
                switch(this.manager.getInterpreterType()){
                
                case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                    return IPythonNature.PYTHON_VERSION_3_0;
                    
                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                    return IPythonNature.JYTHON_VERSION_3_0;
                    
                case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                    return IPythonNature.PYTHON_VERSION_3_0;
                    
                default:
                    throw new RuntimeException("Not python nor jython nor iron python?");
                }
            }
        }
        switch(this.manager.getInterpreterType()){
            
            case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                return IPythonNature.PYTHON_VERSION_LATEST;
                
            case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                return IPythonNature.JYTHON_VERSION_LATEST;
                
            case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                return IPythonNature.PYTHON_VERSION_LATEST;
                
            default:
                throw new RuntimeException("Not python nor jython nor iron python?");
        }
    }

    public String getDefaultVersion() {
        try {
            return getVersion();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    public void setVersion(String version, String interpreter) throws CoreException {
        throw new RuntimeException("Not Implemented: the system nature is read-only.");
    }

    public int getInterpreterType() throws CoreException {
        return this.manager.getInterpreterType();
    }

    public File getCompletionsCacheDir() {
        throw new RuntimeException("Not Implemented");
    }

    public void saveAstManager() {
        throw new RuntimeException("Not Implemented: system nature is only transient.");
    }

    public IPythonPathNature getPythonPathNature() {
        throw new RuntimeException("Not Implemented");
    }

    public String resolveModule(String file) throws MisconfigurationException {
        InterpreterInfo info = (InterpreterInfo) this.manager.getDefaultInterpreterInfo(new NullProgressMonitor());
        if(info == null){
            return null;
        }
        return info.getModulesManager().resolveModule(file);
    }

    public ICodeCompletionASTManager getAstManager() {
        if(systemASTManager == null){
            systemASTManager = new SystemASTManager(this.manager, this, this.info);
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

    
    //builtin completions
    private IToken[] builtinCompletions;
    
    public IToken[] getBuiltinCompletions() {
        return builtinCompletions;
    }

    public void setBuiltinCompletions(IToken[] toks) {
        this.builtinCompletions = toks;
    }
    
    
    //builtin mod
    private IModule builtinMod;

    public IModule getBuiltinMod() {
        return builtinMod;
    }

    public void setBuiltinMod(IModule mod) {
        this.builtinMod = mod;
    }

    
    public int getGrammarVersion() throws MisconfigurationException {
        IInterpreterInfo info;
        if(this.info != null){
            info = this.info;
        }else{
            info = manager.getDefaultInterpreterInfo(new NullProgressMonitor());
        }
        if(info != null){
            return info.getGrammarVersion();
        }else{
            return IPythonNature.LATEST_GRAMMAR_VERSION;
        }
    }

    public IInterpreterInfo getProjectInterpreter() throws MisconfigurationException {
        return this.manager.getDefaultInterpreterInfo(null);
    }

    public boolean isOkToUse(){
        return this.manager != null && this.info != null;
    }
}
