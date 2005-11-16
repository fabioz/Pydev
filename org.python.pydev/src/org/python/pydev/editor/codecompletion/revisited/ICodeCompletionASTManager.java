/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public interface ICodeCompletionASTManager {
    
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
     * Set the project this ast manager works with.
     * 
     * @param project the project related to this ast manager
     * @param restoreDeltas says whether deltas should be restored (if they are not, they should be discarded)
     */
    public abstract void setProject(IProject project, boolean restoreDeltas);
    
    
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

    /**
     * @return the nature associated to this manager
     */
    public abstract IPythonNature getNature();

    //----------------------------------- COMPLETIONS

    /**
     * Returns the imports that start with a given string. The comparisson is not case dependent. Passes all the modules in the cache.
     * 
     * @param initial: this is the initial module (e.g.: foo.bar) or an empty string.
     * @return a Set with the imports as tuples with the name, the docstring.
     */
    public abstract IToken[] getCompletionsForImport(final String original, CompletionRequest request);


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
    public abstract AbstractModule getModule(String name, PythonNature nature, boolean dontSearchInit);

    
    /**
     * @return tuple with:
     * 0: mod
     * 1: tok
     */
    public abstract Tuple<AbstractModule, String> findOnImportedMods( PythonNature nature, String activationToken, AbstractModule current);

    /**
     * This function tries to find some activation token defined in some imported module.  
     * @return tuple with: the module and the token that should be used from it.
     * 
     * @param this is the activation token we have. It may be a single token or some dotted name.
     * 
     * If it is a dotted name, such as testcase.TestCase, we need to match against some import
     * represented as testcase or testcase.TestCase.
     * 
     * If a testcase.TestCase matches against some import named testcase, the import is returned and
     * the TestCase is put as the module
     * 
     * 0: mod
     * 1: tok
     */
    public abstract Tuple<AbstractModule, String> findOnImportedMods( IToken[] importedModules, PythonNature nature, String activationToken, String currentModuleName);


    /**
     * 
     * @param doc
     * @param line
     * @param col
     * @param activationToken
     * @param qualifier
     * @return
     */
    public abstract IToken[] getCompletionsForToken(IDocument doc, CompletionState state);

    /**
     * @param file
     * @param activationToken
     * @param qualifier
     * @param module
     * @param col
     * @param line
     */
    public abstract IToken[] getCompletionsForModule(AbstractModule module, CompletionState state);
    
    /**
     * @param file
     * @param activationToken
     * @param qualifier
     * @param module
     * @param col
     * @param line
     */
    public abstract IToken[] getCompletionsForModule(AbstractModule module, CompletionState state, boolean searchSameLevelMods);

    /**
     * This method gets the completions for a wild import. 
     * They are added to the completions list
     * 
     * @param state this is the completion state
     * @param current this is the current module
     * @param completions OUT this is were completions are added.
     * @param wildImport this is the token identifying the wild import
     */
    public List getCompletionsForWildImport(CompletionState state, AbstractModule current, List completions, IToken wildImport);

    /**
     * This method returns the python builtins as completions
     * 
     * @param state this is the current completion state
     * @param completions OUT this is where the completions are added.
     * @return the same list that has been passed at completions
     */
    public List getBuiltinCompletions(CompletionState state, List completions);

    /**
     * This method can get the global completions for a module (the activation token is usually empty in
     * these cases).
     * 
     * What it actually should do is getting the completions for the wild imported modules, plus builtins,
     * plus others passed as arguments.
     * 
     * @param globalTokens the global tokens found in the module
     * @param importedModules the imported modules
     * @param wildImportedModules the wild imported modules
     * @param state the current completion state
     * @param current the current module
     * @return a list of IToken
     */
    public abstract List getGlobalCompletions(IToken[] globalTokens, IToken[] importedModules, IToken[] wildImportedModules, CompletionState state, AbstractModule current);

    

}