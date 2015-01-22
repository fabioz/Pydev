/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback0;

/**
 * Visitors within pydev should be subclasses of this class.
 *
 * They should be prepared for being reused to, as they are instantiated and reused for visiting many resources.
 *
 * @author Fabio Zadrozny
 */
public abstract class PyDevBuilderVisitor implements Comparable<PyDevBuilderVisitor> {

    public static final int MAX_TO_VISIT_INFINITE = -1;

    /**
     * identifies the key for the module in the cache
     */
    private static final String MODULE_CACHE = "MODULE_CACHE"; //$NON-NLS-1$

    /**
     * identifies the key for the module name in the cache
     */
    private static final String MODULE_NAME_CACHE = "MODULE_NAME"; //$NON-NLS-1$

    /*default*/static final String MODULE_IN_PROJECT_PYTHONPATH = "MODULE_IN_PROJECT_PYTHONPATH"; //$NON-NLS-1$

    /**
     * The default priority is 5.
     *
     * Higher priorities are minor numbers (and vice-versa).
     */
    public static final int PRIORITY_DEFAULT = 5;

    /**
     * Maximum priority is 0
     */
    public static final int PRIORITY_MAX = 0;

    /**
     * Minimum priority is 10
     */
    public static final int PRIORITY_MIN = 10;

    /**
     * Compares them by priority (they are ordered before visiting by priority, so, this can
     * be useful if some visitor needs to run only after some other visitor was executed).
     */
    public int compareTo(PyDevBuilderVisitor o) {
        int priority = getPriority();
        int otherPriority = o.getPriority();
        if (priority < otherPriority) {
            return -1;
        }
        if (otherPriority < priority) {
            return 1;
        }
        return 0; //equal
    }

    /**
     * @return the priority of this visitor (visitors with higher priority --
     * lower numbers -- are visited before)
     */
    protected int getPriority() {
        return PRIORITY_DEFAULT;
    }

    /**
     * This field acts like a memory.
     *
     * It is set before a given resource is visited, and is maintained
     * for each visitor while the same resource is being visited.
     *
     * In this way, we can keep from having to recreate some info (such as the ast) each time over and over
     * for each visitor.
     */
    public VisitorMemo memo;

    /**
     * Constant indicating value in memory to represent a full build.
     */
    public static final String IS_FULL_BUILD = "IS_FULL_BUILD"; //$NON-NLS-1$

    /**
     * Constant indicating value in memory to represent the creation time of the document in memory that the visitor
     * is getting.
     */
    public static final String DOCUMENT_TIME = "DOCUMENT_TIME"; //$NON-NLS-1$

    /**
     * @return whether we are doing a full build right now.
     */
    protected boolean isFullBuild() {
        Boolean b = (Boolean) memo.get(IS_FULL_BUILD);
        if (b == null) {
            return false; // we surely will have it set when it is a full build. (the other way around may not be true).
        }
        return b.booleanValue();
    }

    /**
     * @return The time of the document creation used for this visitor (in current time millis)
     * or -1 if the document creation time is not available.
     */
    protected long getDocumentTime() {
        Long b = (Long) memo.get(DOCUMENT_TIME);
        if (b == null) {
            return -1;
        }
        return b.longValue();
    }

    /**
     * This method returns the module that is created from the given resource.
     *
     * It also uses the cache, to see if the module is already available for that.
     *
     * @param resource the resource we are analyzing
     * @param document the document with the resource contents
     * @return the module that is created by the given resource
     * @throws MisconfigurationException
     */
    protected SourceModule getSourceModule(IResource resource, IDocument document, IPythonNature nature)
            throws MisconfigurationException {
        SourceModule module = (SourceModule) memo.get(MODULE_CACHE + resource.getModificationStamp());
        if (module == null) {
            module = createSoureModule(resource, document, getModuleName(resource, nature));
            setModuleInCache(resource, module);
        }
        return module;
    }

    /**
     * @param module this is the module to set in the cache
     */
    protected void setModuleInCache(IResource resource, IModule module) {
        memo.put(MODULE_CACHE + resource.getModificationStamp(), module);
    }

    /**
     * @param resource
     * @param document
     * @return
     * @throws MisconfigurationException
     */
    protected SourceModule createSoureModule(IResource resource, IDocument document, String moduleName)
            throws MisconfigurationException {
        SourceModule module;
        PythonNature nature = PythonNature.getPythonNature(resource.getProject());
        IFile f = (IFile) resource;
        String file = f.getRawLocation().toOSString();
        module = AbstractModule.createModuleFromDoc(moduleName, new File(file), document, nature, true);
        return module;
    }

    /**
     * @param resource the resource we are analyzing
     * @return the nature associated to the project where the resource is contained
     */
    protected PythonNature getPythonNature(IResource resource) {
        PythonNature pythonNature = PythonNature.getPythonNature(resource);
        return pythonNature;
    }

    /**
     * @param resource must be the resource we are analyzing because it will go to the cache without the resource (only as MODULE_NAME_CACHE)
     * @return the name of the module we are analyzing (given tho resource)
     * @throws MisconfigurationException
     */
    public String getModuleName(IResource resource, IPythonNature nature) throws MisconfigurationException {
        String moduleName = (String) memo.get(getModuleNameCacheKey(resource));
        if (moduleName == null) {
            moduleName = nature.resolveModule(resource);
            if (moduleName != null) {
                setModuleNameInCache(memo, resource, moduleName);
            } else {
                throw new RuntimeException("Unable to resolve module for:" + resource); //$NON-NLS-1$
            }
        }
        return moduleName;
    }

    private static String getModuleNameCacheKey(IResource resource) {
        return MODULE_NAME_CACHE + resource.getModificationStamp();
    }

    /**
     * @param moduleName the module name to set in the cache
     */
    public static void setModuleNameInCache(Map<String, Object> memo, IResource resource, String moduleName) {
        memo.put(getModuleNameCacheKey(resource), moduleName);
    }

    public boolean isResourceInPythonpathProjectSources(IResource resource, IPythonNature nature, boolean addExternal)
            throws CoreException, MisconfigurationException {
        Boolean isInProjectPythonpath = (Boolean) memo.get(MODULE_IN_PROJECT_PYTHONPATH + addExternal);
        if (isInProjectPythonpath == null) {

            //This was simply: String moduleName = nature.resolveModuleOnlyInProjectSources(resource, addExternal);
            //Inlined with the code below because nature.getPythonPathNature().getOnlyProjectPythonPathStr was one of
            //the slowest things when doing a full build.

            List<String> onlyProjectPythonPathLst = memo.getOnlyProjectPythonPathStr(nature, addExternal);

            String resourceOSString = PydevPlugin.getIResourceOSString(resource);
            String moduleName = null;
            if (resourceOSString != null) {
                ICodeCompletionASTManager astManager = nature.getAstManager();
                if (astManager != null) {
                    IModulesManager modulesManager = astManager.getModulesManager();
                    if (modulesManager instanceof ProjectModulesManager) {
                        PythonPathHelper pythonPathHelper = ((ProjectModulesManager) modulesManager)
                                .getPythonPathHelper();
                        moduleName = pythonPathHelper.resolveModule(
                                resourceOSString, false, onlyProjectPythonPathLst, nature.getProject());
                    }
                }
            }

            isInProjectPythonpath = (moduleName != null);
            if (isInProjectPythonpath) {
                setModuleNameInCache(memo, resource, moduleName);
            }

        }
        return isInProjectPythonpath;
    }

    /**
     * @param resource the resource we want to know about
     * @return true if it is in the pythonpath
     */
    public static boolean isInPythonPath(IResource resource) {
        if (resource == null) {
            return false;
        }
        IProject project = resource.getProject();
        PythonNature nature = PythonNature.getPythonNature(project);
        if (project != null && nature != null) {
            ICodeCompletionASTManager astManager = nature.getAstManager();
            if (astManager != null) {
                IModulesManager modulesManager = astManager.getModulesManager();
                return modulesManager.isInPythonPath(resource, project);
            }
        }

        return false;
    }

    /**
     *
     * @return the maximun number of resources that it is allowed to visit (if this
     * number is higher than the number of resources changed, this visitor is not called).
     */
    public int maxResourcesToVisit() {
        return MAX_TO_VISIT_INFINITE;
    }

    /**
     * Called when a resource is changed
     *
     * @param resource to be visited.
     */
    public abstract void visitChangedResource(IResource resource, ICallback0<IDocument> document,
            IProgressMonitor monitor);

    /**
     * Called when a resource is added. Default implementation calls the same method
     * used for change.
     *
     * @param resource to be visited.
     */
    public void visitAddedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        visitChangedResource(resource, document, monitor);
    }

    /**
     * Called when a resource is removed
     *
     * @param resource to be visited.
     */
    public abstract void visitRemovedResource(IResource resource, ICallback0<IDocument> document,
            IProgressMonitor monitor);

    /**
     * This function is called right before a visiting session starts for a delta (end will
     * only be called when the whole delta is processed).
     * @param monitor this is the monitor that will be used in the visit
     * @param nature
     */
    public void visitingWillStart(IProgressMonitor monitor, boolean isFullBuild, IPythonNature nature) {

    }

    /**
     * This function is called when we finish visiting some delta (which may be the whole project or
     * just some files).
     *
     * A use-case is: It may be overridden if we need to store info in a persisting location
     * @param monitor this is the monitor used in the visit
     */
    public void visitingEnded(IProgressMonitor monitor) {

    }
}
