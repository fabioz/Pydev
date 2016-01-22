/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.File;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.shared_core.structure.Tuple;

public interface IModulesManager {
    /**
     * This is the maximun number of deltas that can be generated before saving everything in a big chunck and
     * clearing the deltas
     */
    public static final int MAXIMUN_NUMBER_OF_DELTAS = 100;

    /**
     * @param nature this is the nature for this project modules manager (can be used if no project is set)
     */
    public abstract void setPythonNature(IPythonNature nature);

    /**
     * @return the nature related to this manager
     */
    public abstract IPythonNature getNature();

    /**
     * @return the modules manager that has the system information (for the same type of this modules manager
     * -- e.g. python, jython)
     */
    public abstract ISystemModulesManager getSystemModulesManager();

    /**
     * @param addDependencies: whether we should add the dependencies for this modules manager to the given set
     * of module names returned (or if we should just get the direct dependencies in this manager).
     *
     * @param partStartingWithLowerCase: whether a given part of the module starts with the lower case version
     * of the passed string (e.g.: if mod1.mod2.mod3 will give a match for the string mod3)
     *
     * @return a set with the names of all available modules
     */
    public abstract Set<String> getAllModuleNames(boolean addDependencies, String partStartingWithLowerCase);

    public abstract ModulesKey[] getOnlyDirectModules();

    public abstract IModule getRelativeModule(String name, IPythonNature nature);

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     *
     * @param name the name of the module we're looking for (e.g.: mod1.mod2)
     * @param dontSearchInit is used in a negative form because initially it was isLookingForRelative, but
     * it actually defines if we should look in __init__ modules too, so, the name matches the old signature.
     *
     * NOTE: isLookingForRelative description was: when looking for relative imports, we don't check for __init__
     * @return the module represented by this name or null if not found.
     */
    public abstract IModule getModule(String name, IPythonNature nature, boolean dontSearchInit);

    public abstract IModule getModule(String name, IPythonNature nature, boolean checkSystemManager,
            boolean dontSearchInit);

    /**
     * @param member the member we want to know if it is in the pythonpath
     * @param container the project where the member is
     * @return true if it is in the pythonpath and false otherwise
     */
    public abstract boolean isInPythonPath(IResource member, IProject container);

    /**
     * @param member this is the member file we are analyzing
     * @param container the project where the file is contained
     * @return the name of the module given the pythonpath
     */
    public abstract String resolveModule(IResource member, IProject container);

    /**
     * Resolve module for all, including the system manager.
     *
     * May return null if we're not able to resolve tho module.
     *
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#resolveModule(java.lang.String)
     */
    public abstract String resolveModule(String full);

    /**
     * @param full the full file-system path of the file to resolve
     * @return the name of the module given the pythonpath
     */
    public abstract String resolveModule(String full, boolean checkSystemManager);

    public abstract void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor);

    /**
     * @param addDependenciesSize whether the dependencies of a given modules manager
     *
     * @return the number of modules in this modules manager.
     */
    public abstract int getSize(boolean addDependenciesSize);

    /**
     * Forced builtins are only specified in the system.
     *
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getBuiltins()
     */
    public abstract String[] getBuiltins();

    /**
     * @param interpreter this is the interpreter that should be used for getting the pythonpathString interpreter
     *  (if it is null, the default interpreter is used)
     *
     * @param manager this is the interpreter manager that contains the interpreter passed. It's needed so that we
     *   can get the actual pythonpath for the interpreter passed (needed for the system pythonpath info).
     *
     * @return the paths that constitute the pythonpath as a list of strings
     */
    public abstract List<String> getCompletePythonPath(IInterpreterInfo interpreter, IInterpreterManager manager);

    public abstract SortedMap<ModulesKey, ModulesKey> getAllModulesStartingWith(String moduleToGetTokensFrom);

    public abstract SortedMap<ModulesKey, ModulesKey> getAllDirectModulesStartingWith(String moduleToGetTokensFrom);

    /**
     * @return true if it was started without problems
     */
    public boolean startCompletionCache();

    public void endCompletionCache();

    /**
     * @return the pythonpath helper related to this modules manager. May return null if it doesn't have a related
     * pythonpath helper (e.g.: a modules manager for another kind of project -- such as a java project).
     */
    public abstract Object /*PythonPathHelper*/ getPythonPathHelper();

    /**
     * This method removes some module from this modules manager.
     *
     * @param key the key that represents the module to be removed from this modules manager.
     */
    public abstract void removeModules(Collection<ModulesKey> toRem);

    /**
     * Will add an Empty Module with the given key (will be made concrete only when its actual contents are requested).
     * @return the module created for the given key.
     */
    public abstract IModule addModule(ModulesKey key);

    /**
     * @return a tuple with the IModule requested and the IModulesManager that contained that module.
     * May return null if not found.
     */
    public Tuple<IModule, IModulesManager> getModuleAndRelatedModulesManager(String name, IPythonNature nature,
            boolean checkSystemManager, boolean dontSearchInit);

    /**
     * Used so that we can deal with modules that are not saved (i.e.: modules that we're currently
     * editing but don't want to save).
     *
     * @return the handle to be used to pop it later on.
     */
    public int pushTemporaryModule(String moduleName, IModule module);

    /**
     * Remove a previous pushTemporaryModule.
     */
    public void popTemporaryModule(String moduleName, int handle);

    public void saveToFile(File workspaceMetadataFile);

    public abstract boolean hasModule(ModulesKey key);

    /**
     * I.e.: don't forget to close returned closeable (prefer to use in try block)
     */
    public abstract AutoCloseable withNoGenerateDeltas();

    /**
     * Lock which should be used to get contents from a compiled module from the cache.
     */
    public abstract Object getCompiledModuleCreationLock(String name);

    /**
     * @return a tuple with the new keys to be added to the modules manager (i.e.: found in keysFound but not in the
     * modules manager) and the keys to be removed from the modules manager (i.e.: found in the modules manager but
     * not in the keysFound)
     */
    public abstract Tuple<List<ModulesKey>, List<ModulesKey>> diffModules(
            AbstractMap<ModulesKey, ModulesKey> keysFound);
}
