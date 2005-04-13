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
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.ModulesKey;
import org.python.pydev.plugin.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public interface IASTManager {
    
    /**
     * This method rebuilds the paths that can be used for the code completion.
     * It doesn't load the modules, only the paths. 
     * 
     * @param pythonpath: string with paths separated by |
     * @param project: this is the project that is associated with this manager.
     * @param monitor: monitor for progress.
     */
    public abstract void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor);

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
     * This method synchs the modules from this manager with the filesystem. This means that
     * the modules that don't exist anymore are removed from the memory and from the files that
     * represent a cache.
     * 
     * Modules changed in the filesystem are changed to Empty modules in memory, so that we reload
     * them when requested again.
     * 
     * @param project: this is the project that is associated with this manager.
     * @param monitor: monitor for progress.
     */
    public abstract void syncModules(final IProject project, IProgressMonitor monitor);

    /**
     * Remove all modules below a module represented by a folder.
     * 
     * @param file
     * @param project
     * @param monitor
     */
    public void removeModulesBelow(File file, IProject project, IProgressMonitor monitor);

    //----------------------------------- COMPLETIONS

    /**
     * Returns the imports that start with a given string. The comparisson is not case dependent. Passes all the modules in the cache.
     * 
     * @param initial: this is the initial module (e.g.: foo.bar) or an empty string.
     * @return a Set with the imports as tuples with the name, the docstring.
     */
    public abstract IToken[] getCompletionsForImport(final String original, PythonNature nature);

    /**
     * @return a Set of strings with all the modules.
     */
    public abstract ModulesKey[] getAllModules();

    /**
     * @return the number of modules.
     */
    public abstract int getSize();

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