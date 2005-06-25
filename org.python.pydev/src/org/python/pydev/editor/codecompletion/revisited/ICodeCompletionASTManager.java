/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.management.IASTManager;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public interface ICodeCompletionASTManager extends IASTManager{
    
    /**
     * This method rebuilds the paths that can be used for the code completion.
     * It doesn't load the modules, only the paths. 
     * 
     * @param pythonpath: string with paths separated by |
     * @param project: this is the project that is associated with this manager.
     * @param monitor: monitor for progress.
     */
    public abstract void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor);
    
    
    public abstract void setSystemModuleManager(SystemModulesManager systemManager, IProject project);

    /**
     * This method provides a way to rebuild a module (new delta).
     * 
     * @param file: file that represents a module
     * @param doc
     * @param project: this is the project that is associated with this manager.
     * @param monitor: monitor for progress.
     */
    public abstract void rebuildModule(final File file, final IDocument doc, final IProject project, IProgressMonitor monitor, PythonNature nature);

    /**
     * This method provides a way to remove a module (remove delta).
     * 
     * @param file: file that represents a module
     * @param project: this is the project that is associated with this manager.
     * @param monitor: monitor for progress.
     */
    public abstract void removeModule(final File file, final IProject project, IProgressMonitor monitor);
    
    /**
     * @return the modules manager associated with this manager.
     */
    public abstract ProjectModulesManager getProjectModulesManager();


    //----------------------------------- COMPLETIONS

    /**
     * Returns the imports that start with a given string. The comparisson is not case dependent. Passes all the modules in the cache.
     * 
     * @param initial: this is the initial module (e.g.: foo.bar) or an empty string.
     * @return a Set with the imports as tuples with the name, the docstring.
     */
    public abstract IToken[] getCompletionsForImport(final String original, PythonNature nature);


    /**
     * The completion should work in the following way:
     * 
     * First we have to know in which scope we are.
     * 
     * If we have no token nor qualifier, get the locals for the file (only from module imports or from inner scope).
     * 
     * If we have a part of the qualifier and not activationToken, go for all that match (e.g. all classes, so that we can make the import
     * automatically)
     * 
     * If we have the activationToken, try to guess what it is and get its attrs and funcs.
     * 
     * @param file
     * @param doc
     * @param state
     * @return
     */
    public abstract IToken[] getCompletionsForToken(File file, IDocument doc, CompletionState state);
    
    /**
     * 
     * @param name
     * @param nature
     * @return the module with the specified name.
     */
    public abstract AbstractModule getModule(String name, PythonNature nature);

    
    /**
     * @return tuple with:
     * 0: mod
     * 1: tok
     */
    public abstract Object[] findOnImportedMods( PythonNature nature, String activationToken, AbstractModule current);

}