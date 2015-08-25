/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 11, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.INavigatorFilterService;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.navigator.elements.PythonFile;
import org.python.pydev.navigator.elements.PythonFolder;
import org.python.pydev.navigator.elements.PythonNode;
import org.python.pydev.navigator.elements.PythonProjectSourceFolder;
import org.python.pydev.navigator.elements.PythonResource;
import org.python.pydev.navigator.elements.PythonSourceFolder;
import org.python.pydev.navigator.filters.PythonNodeFilter;
import org.python.pydev.outline.ParsedItem;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.parser.visitors.scope.OutlineCreatorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.IPythonNatureListener;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.PythonNatureListenersManager;
import org.python.pydev.plugin.preferences.PyTitlePreferencesPage;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.outline.IParsedItem;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * A good part of the refresh for the model was gotten from org.eclipse.ui.model.WorkbenchContentProvider
 * (mostly just changed the way to get content changes in python files)
 *
 * There are other important notifications that we need to learn about.
 * Namely:
 *  - When a source folder is created
 *  - When the way to see it changes (flat or not)
 *
 * @author Fabio
 */
public abstract class PythonBaseModelProvider extends BaseWorkbenchContentProvider implements IResourceChangeListener,
        IPythonNatureListener, IPropertyChangeListener {

    /**
     * Object representing an empty array.
     */
    private static final Object[] EMPTY = new Object[0];

    /**
     * Type of the error markers to show in the pydev package explorer.
     */
    public static final String PYDEV_PACKAGE_EXPORER_PROBLEM_MARKER = "org.python.pydev.PydevProjectErrorMarkers";

    /**
     * This is the viewer that we're using to see the contents of this file provider.
     */
    protected CommonViewer viewer;

    /**
     * This is the helper we have to know if the top-level elements shoud be working sets or only projects.
     */
    protected final TopLevelProjectsOrWorkingSetChoice topLevelChoice;

    private ICommonContentExtensionSite aConfig;

    private IWorkspace[] input;

    public static final boolean DEBUG = false;

    /**
     * This callback should return the working sets available.
     *
     * It's done this way (and not final) because we want to mock it on tests.
     */
    protected static ICallback<List<IWorkingSet>, IWorkspaceRoot> getWorkingSetsCallback = new ICallback<List<IWorkingSet>, IWorkspaceRoot>() {
        public List<IWorkingSet> call(IWorkspaceRoot arg) {
            return Arrays.asList(PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets());
        }
    };

    /**
     * Constructor... registers itself as a python nature listener
     */
    public PythonBaseModelProvider() {
        PythonNatureListenersManager.addPythonNatureListener(this);
        if (SharedCorePlugin.inTestMode()) {
            // testing, don't use preference store
        } else {
            PydevPlugin plugin = PydevPlugin.getDefault();
            IPreferenceStore preferenceStore = plugin.getPreferenceStore();
            preferenceStore.addPropertyChangeListener(this);
        }

        //just leave it created
        topLevelChoice = new TopLevelProjectsOrWorkingSetChoice();
    }

    /**
     * Initializes the viewer and the choice for top-level elements.
     */
    public void init(ICommonContentExtensionSite aConfig) {
        this.aConfig = aConfig;
    }

    /**
     * Helper to provide a single update with multiple notifications.
     */
    private class Updater extends Job {

        /**
         * The pythonpath set for the project
         */
        private List<String> projectPythonpath;

        /**
         * The project which had the pythonpath rebuilt
         */
        private IProject project;

        /**
         * Lock for accessing project and projectPythonpath
         */
        private Object updaterLock = new Object();

        public Updater() {
            super("Model provider updating pythonpath");
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            IProject projectToUse;
            List<String> projectPythonpathToUse;
            synchronized (updaterLock) {
                projectToUse = project;
                projectPythonpathToUse = projectPythonpath;

                //Clear the fields (we already have the locals with the values we need.)
                project = null;
                projectPythonpath = null;
            }

            //No need to be synchronized (that's the slow part)
            if (projectToUse != null && projectPythonpathToUse != null) {
                internalDoNotifyPythonPathRebuilt(projectToUse, projectPythonpathToUse);
            }

            return Status.OK_STATUS;
        }

        /**
         * Sets the needed parameters to rebuild the pythonpath.
         */
        public void setNeededParameters(IProject project, List<String> projectPythonpath) {
            synchronized (updaterLock) {
                this.project = project;
                this.projectPythonpath = projectPythonpath;
            }
        }
    }

    /**
     * We need to have one updater per project. After created, it'll remain always there.
     */
    private static Map<IProject, Updater> projectToUpdater = new HashMap<IProject, Updater>();
    private static Object projectToUpdaterLock = new Object();

    private Updater getUpdater(IProject project) {
        synchronized (projectToUpdaterLock) {
            Updater updater = projectToUpdater.get(project);
            if (updater == null) {
                updater = new Updater();
                projectToUpdater.put(project, updater);
            }
            return updater;
        }
    }

    /**
     * Helper so that we can have many notifications and create a single request.
     * @param projectPythonpath
     * @param project
     */
    private void createAndStartUpdater(IProject project, List<String> projectPythonpath) {
        Updater updater = getUpdater(project);
        updater.setNeededParameters(project, projectPythonpath);
        updater.schedule(200);
    }

    /**
     * Notification received when the pythonpath has been changed or rebuilt.
     */
    public void notifyPythonPathRebuilt(IProject project, IPythonNature nature) {
        if (project == null) {
            return;
        }

        List<String> projectPythonpath;
        if (nature == null) {
            //the nature has just been removed.
            projectPythonpath = new ArrayList<String>();
        } else {
            try {
                projectPythonpath = nature.getPythonPathNature().getCompleteProjectPythonPath(
                        nature.getProjectInterpreter(), nature.getRelatedInterpreterManager());
            } catch (PythonNatureWithoutProjectException e) {
                projectPythonpath = new ArrayList<String>();
            } catch (MisconfigurationException e) {
                projectPythonpath = new ArrayList<String>();
            }
        }

        createAndStartUpdater(project, projectPythonpath);
    }

    public void propertyChange(PropertyChangeEvent event) {
        //When a property that'd change an icon changes, the tree must be updated.
        String property = event.getProperty();
        if (PyTitlePreferencesPage.isTitlePreferencesIconRelatedProperty(property)) {
            IWorkspace[] localInput = this.input;
            if (localInput != null) {
                for (IWorkspace iWorkspace : localInput) {
                    IWorkspaceRoot root = iWorkspace.getRoot();
                    if (root != null) {
                        //Update all children too (getUpdateRunnable wouldn't update children)
                        Runnable runnable = getRefreshRunnable(root);

                        final Collection<Runnable> runnables = new ArrayList<Runnable>();
                        runnables.add(runnable);
                        processRunnables(runnables);
                    }
                }
            }

        }
    }

    /**
     * This is the actual implementation of the rebuild.
     *
     * @return the element that should be refreshed or null if the project location can't be determined!
     */
    /*default*/IResource internalDoNotifyPythonPathRebuilt(IProject project, List<String> projectPythonpath) {
        IResource refreshObject = project;
        IPath location = project.getLocation();
        if (location == null) {
            return null;
        }

        if (DEBUG) {
            System.out.println("\n\nRebuilding pythonpath: " + project + " - " + projectPythonpath);
        }
        HashSet<Path> projectPythonpathSet = new HashSet<Path>();

        for (String string : projectPythonpath) {
            Path newPath = new Path(string);
            if (location.equals(newPath)) {
                refreshObject = project.getParent();
            }
            projectPythonpathSet.add(newPath);
        }

        ProjectInfoForPackageExplorer projectInfo = ProjectInfoForPackageExplorer.getProjectInfo(project);
        if (projectInfo != null) {
            projectInfo.recreateInfo(project);

            Set<PythonSourceFolder> existingSourceFolders = projectInfo.sourceFolders;
            if (existingSourceFolders != null) {
                //iterate in a copy
                for (PythonSourceFolder pythonSourceFolder : new HashSet<PythonSourceFolder>(existingSourceFolders)) {
                    IPath fullPath = pythonSourceFolder.container.getLocation();
                    if (!projectPythonpathSet.contains(fullPath)) {
                        if (pythonSourceFolder instanceof PythonProjectSourceFolder) {
                            refreshObject = project.getParent();
                        }
                        existingSourceFolders.remove(pythonSourceFolder);//it's not a valid source folder anymore...
                        if (DEBUG) {
                            System.out.println("Removing:" + pythonSourceFolder + " - " + fullPath);
                        }
                    }
                }
            }
        }

        Runnable refreshRunnable = getRefreshRunnable(refreshObject);
        final Collection<Runnable> runnables = new ArrayList<Runnable>();
        runnables.add(refreshRunnable);
        processRunnables(runnables);
        return refreshObject;
    }

    /**
     * @see PythonModelProvider#getResourceInPythonModel(IResource, boolean, boolean)
     */
    protected Object getResourceInPythonModel(IResource object) {
        return getResourceInPythonModel(object, false, false);
    }

    /**
     * @see PythonModelProvider#getResourceInPythonModel(IResource, boolean, boolean)
     */
    protected Object getResourceInPythonModel(IResource object, boolean returnNullIfNotFound) {
        return getResourceInPythonModel(object, false, returnNullIfNotFound);
    }

    /**
     * Given some IResource in the filesystem, return the representation for it in the python model
     * or the resource itself if it could not be found in the python model.
     *
     * Note that this method only returns some resource already created (it does not
     * create some resource if it still does not exist)
     */
    protected Object getResourceInPythonModel(IResource object, boolean removeFoundResource,
            boolean returnNullIfNotFound) {
        if (DEBUG) {
            System.out.println("Getting resource in python model:" + object);
        }
        Set<PythonSourceFolder> sourceFolders = getProjectSourceFolders(object.getProject());
        Object f = null;
        PythonSourceFolder sourceFolder = null;

        for (Iterator<PythonSourceFolder> it = sourceFolders.iterator(); f == null && it.hasNext();) {
            sourceFolder = it.next();
            if (sourceFolder.getActualObject().equals(object)) {
                f = sourceFolder;
            } else {
                f = sourceFolder.getChild(object);
            }
        }
        if (f == null) {
            if (returnNullIfNotFound) {
                return null;
            } else {
                return object;
            }
        } else {
            if (removeFoundResource) {
                if (f == sourceFolder) {
                    sourceFolders.remove(f);
                } else {
                    sourceFolder.removeChild(object);
                }
            }
        }
        return f;
    }

    /**
     * @param object: the resource we're interested in
     * @return a set with the PythonSourceFolder that exist in the project that contains it
     */
    protected Set<PythonSourceFolder> getProjectSourceFolders(IProject project) {
        ProjectInfoForPackageExplorer projectInfo = ProjectInfoForPackageExplorer.getProjectInfo(project);
        if (projectInfo != null) {
            return projectInfo.sourceFolders;
        }
        return new HashSet<PythonSourceFolder>();
    }

    /**
     * @return the parent for some element.
     */
    @Override
    public Object getParent(Object element) {
        if (DEBUG) {
            System.out.println("getParent for: " + element);
        }

        Object parent = null;
        //Now, we got the parent for the resources correctly at this point, but there's one last thing we may need to
        //do: the actual parent may be a working set!
        Object p = this.topLevelChoice.getWorkingSetParentIfAvailable(element, getWorkingSetsCallback);
        if (p != null) {
            parent = p;

        } else if (element instanceof IWrappedResource) {
            // just return the parent
            IWrappedResource resource = (IWrappedResource) element;
            parent = resource.getParentElement();

        } else if (element instanceof IWorkingSet) {
            parent = ResourcesPlugin.getWorkspace().getRoot();

        } else if (element instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) element;
            return treeNode.getParent();
        }

        if (parent == null) {
            parent = super.getParent(element);
        }
        if (DEBUG) {
            System.out.println("getParent RETURN: " + parent);
        }
        return parent;
    }

    /**
     * @return whether there are children for the given element. Note that there is
     * an optimization in this method, so that it works correctly for elements that
     * are not python files, and returns true if it is a python file with any content
     * (even if that content does not actually map to a node.
     *
     * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#hasChildren(java.lang.Object)
     */
    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof PythonFile) {
            //If we're not showing nodes, return false.
            INavigatorContentService contentService = viewer.getNavigatorContentService();
            INavigatorFilterService filterService = contentService.getFilterService();
            ViewerFilter[] visibleFilters = filterService.getVisibleFilters(true);
            for (ViewerFilter viewerFilter : visibleFilters) {
                if (viewerFilter instanceof PythonNodeFilter) {
                    return false;
                }
            }

            PythonFile f = (PythonFile) element;
            if (PythonPathHelper.isValidSourceFile(f.getActualObject())) {
                try {
                    InputStream contents = f.getContents();
                    try {
                        if (contents.read() == -1) {
                            return false; //if there is no content in the file, it has no children
                        } else {
                            return true; //if it has any content, it has children (performance reasons)
                        }
                    } finally {
                        contents.close();
                    }
                } catch (Exception e) {
                    Log.log("Handled error getting contents.", e);
                    return false;
                }
            }
            return false;
        }
        if (element instanceof TreeNode<?>) {
            TreeNode<?> treeNode = (TreeNode<?>) element;
            return treeNode.hasChildren();
        }
        return getChildren(element).length > 0;
    }

    /**
     * The inputs for this method are:
     *
     * IWorkingSet (in which case it will return the projects -- IResource -- that are a part of the working set)
     * IResource (in which case it will return IWrappedResource or IResources)
     * IWrappedResource (in which case it will return IWrappedResources)
     *
     * @return the children for some element (IWrappedResource or IResource)
     */
    @Override
    public Object[] getChildren(Object parentElement) {
        if (DEBUG) {
            System.out.println("getChildren for: " + parentElement);
        }
        Object[] childrenToReturn = null;

        if (parentElement instanceof IWrappedResource) {
            // we're below some python model
            childrenToReturn = getChildrenForIWrappedResource((IWrappedResource) parentElement);

        } else if (parentElement instanceof IResource) {
            // now, this happens if we're not below a python model(so, we may only find a source folder here)
            childrenToReturn = getChildrenForIResourceOrWorkingSet(parentElement);

        } else if (parentElement instanceof IWorkspaceRoot) {
            switch (topLevelChoice.getRootMode()) {
                case TopLevelProjectsOrWorkingSetChoice.WORKING_SETS:
                    IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
                    if (workingSets == null || workingSets.length == 0) {
                        TreeNode noWorkingSets = createErrorNoWorkingSetsDefined(parentElement);
                        return new Object[] { noWorkingSets };
                    }
                    return workingSets;
                case TopLevelProjectsOrWorkingSetChoice.PROJECTS:
                    //Just go on...
            }

        } else if (parentElement instanceof IWorkingSet) {
            if (parentElement instanceof IWorkingSet) {
                IWorkingSet workingSet = (IWorkingSet) parentElement;
                childrenToReturn = workingSet.getElements();
                if (childrenToReturn == null || childrenToReturn.length == 0) {
                    childrenToReturn = new Object[] { createErrorWorkingSetWithoutChildren(workingSet) };
                }
            }

        } else if (parentElement instanceof TreeNode<?>) {
            TreeNode<?> treeNode = (TreeNode<?>) parentElement;
            return treeNode.getChildren().toArray();
        }

        if (childrenToReturn == null) {
            childrenToReturn = EMPTY;
        }
        if (DEBUG) {
            System.out.println("getChildren RETURN: " + childrenToReturn);
        }
        return childrenToReturn;
    }

    private TreeNode<LabelAndImage> createErrorWorkingSetWithoutChildren(IWorkingSet parentElement) {
        Image img = SharedUiPlugin.getImageCache().get(UIConstants.WARNING);
        TreeNode<LabelAndImage> root = new TreeNode<LabelAndImage>(parentElement,
                new LabelAndImage("Warning: working set: " + parentElement.getName() + " does not have any contents.",
                        img));
        new TreeNode<>(root, new LabelAndImage(
                "Access the menu (Ctrl+F10) to edit the working set.", null));
        new TreeNode<>(root, new LabelAndImage(
                "Or select the working set in the tree and use Alt+Enter.", null));
        return root;
    }

    public TreeNode<LabelAndImage> createErrorNoWorkingSetsDefined(Object parentElement) {
        Image img = SharedUiPlugin.getImageCache().get(UIConstants.WARNING);
        TreeNode<LabelAndImage> root = new TreeNode<LabelAndImage>(parentElement,
                new LabelAndImage("Warning: Top level elements set to working sets but no working sets are defined.",
                        img));
        new TreeNode<>(root, new LabelAndImage(
                "Access the menu (Ctrl+F10) to change to show projects or create a working set.", null));
        return root;
    }

    /**
     * @param parentElement an IResource from where we want to get the children (or a working set)
     *
     * @return as we're not below a source folder here, we have still not entered the 'python' domain,
     * and as the starting point for the 'python' domain is always a source folder, the things
     * that can be returned are IResources and PythonSourceFolders.
     */
    private Object[] getChildrenForIResourceOrWorkingSet(Object parentElement) {
        PythonNature nature = null;
        IProject project = null;
        if (parentElement instanceof IResource) {
            project = ((IResource) parentElement).getProject();
        }

        //we can only get the nature if the project is open
        if (project != null && project.isOpen()) {
            nature = PythonNature.getPythonNature(project);
        }

        //replace folders -> source folders (we should only get here on a path that's not below a source folder)
        Object[] childrenToReturn = super.getChildren(parentElement);

        //if we don't have a python nature in this project, there is no way we can have a PythonSourceFolder
        List<Object> ret = new ArrayList<Object>(childrenToReturn.length);
        for (int i = 0; i < childrenToReturn.length; i++) {
            PythonNature localNature = nature;
            IProject localProject = project;

            //now, first we have to try to get it (because it might already be created)
            Object child = childrenToReturn[i];

            if (child == null) {
                continue;
            }

            //only add it if it wasn't null
            ret.add(child);

            if (!(child instanceof IResource)) {
                //not an element that we can treat in pydev (but still, it was already added)
                continue;
            }
            child = getResourceInPythonModel((IResource) child);

            if (child == null) {
                //ok, it was not in the python model (but it was already added with the original representation, so, that's ok)
                continue;
            } else {
                ret.set(ret.size() - 1, child); //replace the element added for the one in the python model
            }

            //if it is a folder (that is not already a PythonSourceFolder, it might be that we have to create a PythonSourceFolder)
            if (child instanceof IContainer && !(child instanceof PythonSourceFolder)) {
                IContainer container = (IContainer) child;

                try {
                    //check if it is a source folder (and if it is, create it)
                    if (localNature == null) {
                        if (container instanceof IProject) {
                            localProject = (IProject) container;
                            if (localProject.isOpen() == false) {
                                continue;
                            } else {
                                localNature = PythonNature.getPythonNature(localProject);
                            }
                        } else {
                            continue;
                        }
                    }
                    //if it's a python project, the nature can't be null
                    if (localNature == null) {
                        continue;
                    }

                    Set<String> sourcePathSet = localNature.getPythonPathNature().getProjectSourcePathSet(true);
                    IPath fullPath = container.getFullPath();
                    if (sourcePathSet.contains(fullPath.toString())) {
                        PythonSourceFolder createdSourceFolder;
                        if (container instanceof IFolder) {
                            createdSourceFolder = new PythonSourceFolder(parentElement, (IFolder) container);
                        } else if (container instanceof IProject) {
                            createdSourceFolder = new PythonProjectSourceFolder(parentElement, (IProject) container);
                        } else {
                            throw new RuntimeException("Should not get here.");
                        }
                        ret.set(ret.size() - 1, createdSourceFolder); //replace the element added for the one in the python model
                        Set<PythonSourceFolder> sourceFolders = getProjectSourceFolders(localProject);
                        sourceFolders.add(createdSourceFolder);
                    }
                } catch (CoreException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return ret.toArray();
    }

    /**
     * @param wrappedResourceParent: this is the parent that is an IWrappedResource (which means
     * that children will also be IWrappedResources)
     *
     * @return the children (an array of IWrappedResources)
     */
    private Object[] getChildrenForIWrappedResource(IWrappedResource wrappedResourceParent) {
        //------------------------------------------------------------------- get python nature
        PythonNature nature = null;
        Object[] childrenToReturn = null;
        Object obj = wrappedResourceParent.getActualObject();
        IProject project = null;
        if (obj instanceof IResource) {
            IResource resource = (IResource) obj;
            project = resource.getProject();
            if (project != null && project.isOpen()) {
                nature = PythonNature.getPythonNature(project);
            }
        }

        //------------------------------------------------------------------- treat python nodes
        if (wrappedResourceParent instanceof PythonNode) {
            PythonNode node = (PythonNode) wrappedResourceParent;
            childrenToReturn = getChildrenFromParsedItem(wrappedResourceParent, node.entry, node.pythonFile);

            //------------------------------------- treat python files (add the classes/methods,etc)
        } else if (wrappedResourceParent instanceof PythonFile) {
            // if it's a file, we want to show the classes and methods
            PythonFile file = (PythonFile) wrappedResourceParent;
            if (PythonPathHelper.isValidSourceFile(file.getActualObject())) {

                if (nature != null) {
                    ICodeCompletionASTManager astManager = nature.getAstManager();
                    //the nature may still not be completely restored...
                    if (astManager != null) {
                        IModulesManager modulesManager = astManager.getModulesManager();

                        if (modulesManager instanceof IProjectModulesManager) {
                            IProjectModulesManager projectModulesManager = (IProjectModulesManager) modulesManager;
                            String moduleName = projectModulesManager.resolveModuleInDirectManager(file
                                    .getActualObject());
                            if (moduleName != null) {
                                IModule module = projectModulesManager.getModuleInDirectManager(moduleName, nature,
                                        true);
                                if (module == null) {
                                    //ok, something strange happened... it shouldn't be null... maybe empty, but not null at this point
                                    //so, if it exists, let's try to create it...
                                    //TODO: This should be moved to somewhere else.
                                    String resourceOSString = PydevPlugin.getIResourceOSString(file.getActualObject());
                                    if (resourceOSString != null) {
                                        File f = new File(resourceOSString);
                                        if (f.exists()) {
                                            projectModulesManager.addModule(new ModulesKey(moduleName, f));
                                            module = projectModulesManager.getModuleInDirectManager(moduleName, nature,
                                                    true);
                                        }
                                    }
                                }
                                if (module instanceof SourceModule) {
                                    SourceModule sourceModule = (SourceModule) module;

                                    OutlineCreatorVisitor visitor = OutlineCreatorVisitor.create(sourceModule.getAst());
                                    ParsedItem root = new ParsedItem(visitor.getAll().toArray(
                                            new ASTEntryWithChildren[0]), null);
                                    childrenToReturn = getChildrenFromParsedItem(wrappedResourceParent, root, file);
                                }
                            }
                        }
                    }
                }
            }
        }

        //------------------------------------------------------------- treat folders and others
        else {
            Object[] children = super.getChildren(wrappedResourceParent.getActualObject());
            childrenToReturn = wrapChildren(wrappedResourceParent, wrappedResourceParent.getSourceFolder(), children);
        }
        return childrenToReturn;
    }

    /**
     * This method changes the contents of the children so that the actual types are mapped to
     * elements of our python model.
     *
     * @param parent the parent (from the python model)
     * @param pythonSourceFolder this is the source folder that contains this resource
     * @param children these are the children thot should be wrapped (note that this array
     * is not actually changed -- a new array is created and returned).
     *
     * @return an array with the wrapped types
     */
    protected Object[] wrapChildren(IWrappedResource parent, PythonSourceFolder pythonSourceFolder, Object[] children) {
        List<Object> ret = new ArrayList<Object>(children.length);

        for (int i = 0; i < children.length; i++) {
            Object object = children[i];

            if (object instanceof IResource) {
                Object existing = getResourceInPythonModel((IResource) object, true);
                if (existing == null) {

                    if (object instanceof IFolder) {
                        object = new PythonFolder(parent, ((IFolder) object), pythonSourceFolder);

                    } else if (object instanceof IFile) {
                        object = new PythonFile(parent, ((IFile) object), pythonSourceFolder);

                    } else if (object instanceof IResource) {
                        object = new PythonResource(parent, (IResource) object, pythonSourceFolder);
                    }
                } else { //existing != null
                    object = existing;
                }
            }

            if (object == null) {
                continue;
            } else {
                ret.add(object);
            }

        }
        return ret.toArray();
    }

    /**
     * @param parentElement this is the elements returned
     * @param root this is the parsed item that has children that we want to return
     * @return the children elements (PythonNode) for the passed parsed item
     */
    private Object[] getChildrenFromParsedItem(Object parentElement, ParsedItem root, PythonFile pythonFile) {
        IParsedItem[] children = root.getChildren();

        PythonNode p[] = new PythonNode[children.length];
        int i = 0;
        // in this case, we just want to return the roots
        for (IParsedItem e : children) {
            p[i] = new PythonNode(pythonFile, parentElement, (ParsedItem) e);
            i++;
        }
        return p;
    }

    /*
     * (non-Javadoc) Method declared on IContentProvider.
     */
    @Override
    public void dispose() {
        try {
            if (viewer != null) {
                IWorkspace[] workspace = null;
                Object obj = viewer.getInput();
                if (obj instanceof IWorkspace) {
                    workspace = new IWorkspace[] { (IWorkspace) obj };
                } else if (obj instanceof IContainer) {
                    workspace = new IWorkspace[] { ((IContainer) obj).getWorkspace() };
                } else if (obj instanceof IWorkingSet) {
                    IWorkingSet newWorkingSet = (IWorkingSet) obj;
                    workspace = getWorkspaces(newWorkingSet);
                }

                if (workspace != null) {
                    for (IWorkspace w : workspace) {
                        w.removeResourceChangeListener(this);
                    }
                }
            }

        } catch (Exception e) {
            Log.log(e);
        }

        try {
            PythonNatureListenersManager.removePythonNatureListener(this);
        } catch (Exception e) {
            Log.log(e);
        }

        try {
            this.topLevelChoice.dispose();
        } catch (Exception e) {
            Log.log(e);
        }

        try {
            super.dispose();
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /*
     * (non-Javadoc) Method declared on IContentProvider.
     */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        super.inputChanged(viewer, oldInput, newInput);

        this.viewer = (CommonViewer) viewer;

        //whenever the input changes, we must reconfigure the top level choice
        topLevelChoice.init(aConfig, this.viewer);

        IWorkspace[] oldWorkspace = null;
        IWorkspace[] newWorkspace = null;

        //get the old
        if (oldInput instanceof IWorkspace) {
            oldWorkspace = new IWorkspace[] { (IWorkspace) oldInput };
        } else if (oldInput instanceof IResource) {
            oldWorkspace = new IWorkspace[] { ((IResource) oldInput).getWorkspace() };
        } else if (oldInput instanceof IWrappedResource) {
            IWrappedResource iWrappedResource = (IWrappedResource) oldInput;
            Object actualObject = iWrappedResource.getActualObject();
            if (actualObject instanceof IResource) {
                IResource iResource = (IResource) actualObject;
                oldWorkspace = new IWorkspace[] { iResource.getWorkspace() };
            }
        } else if (oldInput instanceof IWorkingSet) {
            IWorkingSet oldWorkingSet = (IWorkingSet) oldInput;
            oldWorkspace = getWorkspaces(oldWorkingSet);
        }

        //and the new
        if (newInput instanceof IWorkspace) {
            newWorkspace = new IWorkspace[] { (IWorkspace) newInput };
        } else if (newInput instanceof IResource) {
            newWorkspace = new IWorkspace[] { ((IResource) newInput).getWorkspace() };
        } else if (newInput instanceof IWrappedResource) {
            IWrappedResource iWrappedResource = (IWrappedResource) newInput;
            Object actualObject = iWrappedResource.getActualObject();
            if (actualObject instanceof IResource) {
                IResource iResource = (IResource) actualObject;
                newWorkspace = new IWorkspace[] { iResource.getWorkspace() };
            }
        } else if (newInput instanceof IWorkingSet) {
            IWorkingSet newWorkingSet = (IWorkingSet) newInput;
            newWorkspace = getWorkspaces(newWorkingSet);
        }

        //now, let's treat the workspace
        if (oldWorkspace != null) {
            for (IWorkspace workspace : oldWorkspace) {
                workspace.removeResourceChangeListener(this);
            }
        }
        if (newWorkspace != null) {
            for (IWorkspace workspace : newWorkspace) {
                workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
            }
        }
        this.input = newWorkspace;
    }

    /**
     * @param newWorkingSet
     */
    private IWorkspace[] getWorkspaces(IWorkingSet newWorkingSet) {
        IAdaptable[] elements = newWorkingSet.getElements();
        HashSet<IWorkspace> set = new HashSet<IWorkspace>();

        for (IAdaptable adaptable : elements) {
            IResource adapter = adaptable.getAdapter(IResource.class);
            if (adapter != null) {
                IWorkspace workspace = adapter.getWorkspace();
                set.add(workspace);
            } else {
                Log.log("Was not expecting that IWorkingSet adaptable didn't return anything...");
            }
        }
        return set.toArray(new IWorkspace[0]);
    }

    /*
     * (non-Javadoc) Method declared on IResourceChangeListener.
     */
    public final void resourceChanged(final IResourceChangeEvent event) {
        processDelta(event.getDelta());
    }

    /**
     * Process the resource delta.
     *
     * @param delta
     */
    protected void processDelta(IResourceDelta delta) {
        Control ctrl = viewer.getControl();
        if (ctrl == null || ctrl.isDisposed()) {
            return;
        }

        final Collection<Runnable> runnables = new ArrayList<Runnable>();
        processDelta(delta, runnables);
        processRunnables(runnables);
    }

    /**
     * @param runnables
     */
    private void processRunnables(final Collection<Runnable> runnables) {
        if (viewer == null) {
            return;
        }

        Control ctrl = viewer.getControl();
        if (ctrl == null || ctrl.isDisposed()) {
            return;
        }
        if (runnables.isEmpty()) {
            return;
        }

        // Are we in the UIThread? If so spin it until we are done
        if (ctrl.getDisplay().getThread() == Thread.currentThread()) {
            runUpdates(runnables);
        } else {
            ctrl.getDisplay().asyncExec(new Runnable() {
                /*
                 * (non-Javadoc)
                 *
                 * @see java.lang.Runnable#run()
                 */
                public void run() {
                    runUpdates(runnables);
                }
            });
        }
    }

    private final Object lock = new Object();
    private final Collection<Runnable> delayedRunnableUpdates = new ArrayList<Runnable>(); //Vector because we want it synchronized!

    /**
     * Run all of the runnable that are the widget updates (or delay them to the next request).
     */
    private void runUpdates(Collection<Runnable> runnables) {
        // Abort if this happens after disposes
        Control ctrl = viewer.getControl();
        if (ctrl == null || ctrl.isDisposed()) {
            synchronized (lock) {
                delayedRunnableUpdates.clear();
            }
            return;
        }

        synchronized (lock) {
            delayedRunnableUpdates.addAll(runnables);
        }
        if (viewer.isBusy()) {
            return; //leave it for the next update!
        }
        ArrayList<Runnable> runnablesToRun = new ArrayList<Runnable>();
        synchronized (lock) {
            runnablesToRun.addAll(delayedRunnableUpdates);
            delayedRunnableUpdates.clear();
        }
        Iterator<Runnable> runnableIterator = runnablesToRun.iterator();
        while (runnableIterator.hasNext()) {
            Runnable runnable = runnableIterator.next();
            runnable.run();
        }
    }

    private final IResource[] EMPTY_RESOURCE_ARRAY = new IResource[0];

    /**
     * Process a resource delta. Add any runnables
     */
    private void processDelta(final IResourceDelta delta, final Collection<Runnable> runnables) {
        // he widget may have been destroyed
        // by the time this is run. Check for this and do nothing if so.
        Control ctrl = viewer.getControl();
        if (ctrl == null || ctrl.isDisposed()) {
            return;
        }

        // Get the affected resource
        final IResource resource = delta.getResource();

        // If any children have changed type, just do a full refresh of this
        // parent,
        // since a simple update on such children won't work,
        // and trying to map the change to a remove and add is too dicey.
        // The case is: folder A renamed to existing file B, answering yes to
        // overwrite B.
        IResourceDelta[] affectedChildren = delta.getAffectedChildren(IResourceDelta.CHANGED);
        for (int i = 0; i < affectedChildren.length; i++) {
            if ((affectedChildren[i].getFlags() & IResourceDelta.TYPE) != 0) {
                runnables.add(getRefreshRunnable(resource));
                return;
            }
        }

        // Opening a project just affects icon, but we need to refresh when
        // a project is closed because if child items have not yet been created
        // in the tree we still need to update the item's children
        int changeFlags = delta.getFlags();
        if ((changeFlags & IResourceDelta.OPEN) != 0) {
            if (resource.isAccessible()) {
                runnables.add(getUpdateRunnable(resource));
            } else {
                runnables.add(getRefreshRunnable(resource));
                return;
            }
        }
        // Check the flags for changes the Navigator cares about.
        // See ResourceLabelProvider for the aspects it cares about.
        // Notice we don't care about F_CONTENT or F_MARKERS currently.
        if ((changeFlags & (IResourceDelta.SYNC | IResourceDelta.TYPE | IResourceDelta.DESCRIPTION)) != 0) {
            runnables.add(getUpdateRunnable(resource));
        }
        // Replacing a resource may affect its label and its children
        if ((changeFlags & IResourceDelta.REPLACED) != 0) {
            runnables.add(getRefreshRunnable(resource));
            return;
        }

        // Replacing a resource may affect its label and its children
        if ((changeFlags & (IResourceDelta.CHANGED | IResourceDelta.CONTENT)) != 0) {
            if (resource instanceof IFile) {
                IFile file = (IFile) resource;
                if (PythonPathHelper.isValidSourceFile(file)) {
                    runnables.add(getRefreshRunnable(resource));
                }
            }
            return;
        }

        // Handle changed children .
        for (int i = 0; i < affectedChildren.length; i++) {
            processDelta(affectedChildren[i], runnables);
        }

        // @issue several problems here:
        // - should process removals before additions, to avoid multiple equal
        // elements in viewer
        // - Kim: processing removals before additions was the indirect cause of
        // 44081 and its varients
        // - Nick: no delta should have an add and a remove on the same element,
        // so processing adds first is probably OK
        // - using setRedraw will cause extra flashiness
        // - setRedraw is used even for simple changes
        // - to avoid seeing a rename in two stages, should turn redraw on/off
        // around combined removal and addition
        // - Kim: done, and only in the case of a rename (both remove and add
        // changes in one delta).

        IResourceDelta[] addedChildren = delta.getAffectedChildren(IResourceDelta.ADDED);
        IResourceDelta[] removedChildren = delta.getAffectedChildren(IResourceDelta.REMOVED);

        if (addedChildren.length == 0 && removedChildren.length == 0) {
            return;
        }

        final IResource[] addedObjects;
        final IResource[] removedObjects;

        // Process additions before removals as to not cause selection
        // preservation prior to new objects being added
        // Handle added children. Issue one update for all insertions.
        int numMovedFrom = 0;
        int numMovedTo = 0;
        if (addedChildren.length > 0) {
            addedObjects = new IResource[addedChildren.length];
            for (int i = 0; i < addedChildren.length; i++) {
                final IResourceDelta addedChild = addedChildren[i];
                addedObjects[i] = addedChild.getResource();
                if (checkInit(addedObjects[i], runnables)) {
                    return; // If true, it means a refresh for the parent was issued!
                }
                if ((addedChild.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
                    ++numMovedFrom;
                }
            }
        } else {
            addedObjects = EMPTY_RESOURCE_ARRAY;
        }

        // Handle removed children. Issue one update for all removals.
        if (removedChildren.length > 0) {
            removedObjects = new IResource[removedChildren.length];
            for (int i = 0; i < removedChildren.length; i++) {
                final IResourceDelta removedChild = removedChildren[i];
                removedObjects[i] = removedChild.getResource();
                if (checkInit(removedObjects[i], runnables)) {
                    return; // If true, it means a refresh for the parent was issued!
                }
                if ((removedChild.getFlags() & IResourceDelta.MOVED_TO) != 0) {
                    ++numMovedTo;
                }
            }
        } else {
            removedObjects = EMPTY_RESOURCE_ARRAY;
        }
        // heuristic test for items moving within same folder (i.e. renames)
        final boolean hasRename = numMovedFrom > 0 && numMovedTo > 0;

        Runnable addAndRemove = new Runnable() {
            public void run() {
                if (viewer instanceof AbstractTreeViewer) {
                    AbstractTreeViewer treeViewer = viewer;
                    // Disable redraw until the operation is finished so we don't
                    // get a flash of both the new and old item (in the case of
                    // rename)
                    // Only do this if we're both adding and removing files (the
                    // rename case)
                    if (hasRename) {
                        treeViewer.getControl().setRedraw(false);
                    }
                    try {
                        Set<IProject> notifyRebuilt = new HashSet<IProject>();

                        //now, we have to make a bridge among the tree and
                        //the python model (so, if some element is removed,
                        //we have to create an actual representation for it)
                        if (addedObjects.length > 0) {
                            treeViewer.add(resource, addedObjects);
                            for (Object object : addedObjects) {
                                if (object instanceof IResource) {
                                    IResource rem = (IResource) object;
                                    Object remInPythonModel = getResourceInPythonModel(rem, true);
                                    if (remInPythonModel instanceof PythonSourceFolder) {
                                        notifyRebuilt.add(rem.getProject());
                                    }
                                }
                            }
                        }

                        if (removedObjects.length > 0) {
                            treeViewer.remove(removedObjects);
                            for (Object object : removedObjects) {
                                if (object instanceof IResource) {
                                    IResource rem = (IResource) object;
                                    Object remInPythonModel = getResourceInPythonModel(rem, true);
                                    if (remInPythonModel instanceof PythonSourceFolder) {
                                        notifyRebuilt.add(rem.getProject());
                                    }
                                }
                            }
                        }

                        for (IProject project : notifyRebuilt) {
                            PythonNature nature = PythonNature.getPythonNature(project);
                            if (nature != null) {
                                notifyPythonPathRebuilt(project, nature);
                            }
                        }
                    } finally {
                        if (hasRename) {
                            treeViewer.getControl().setRedraw(true);
                        }
                    }
                } else {
                    ((StructuredViewer) viewer).refresh(resource);
                }
            }
        };
        runnables.add(addAndRemove);
    }

    /**
     * Checks if a given resource is an __init__ file and if it is, updates its parent (because its icon may have changed)
     * @return
     */
    private boolean checkInit(final IResource resource, final Collection<Runnable> runnables) {
        if (resource != null) {
            String name = resource.getName();
            if (name != null) {
                for (String init : FileTypesPreferencesPage.getValidInitFiles()) {
                    if (name.equals(init)) {
                        //we must make an actual refresh (and not only update) because it'll affect all the children too.
                        runnables.add(getRefreshRunnable(resource.getParent()));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return a runnable for refreshing a resource. Handles structural changes.
     */
    private Runnable getRefreshRunnable(final IResource resource) {
        return new Runnable() {
            public void run() {
                ((StructuredViewer) viewer).refresh(getResourceInPythonModel(resource));
            }
        };
    }

    /**
     * Return a runnable for updating a resource. Does not handle structural changes.
     */
    private Runnable getUpdateRunnable(final IResource resource) {
        return new Runnable() {
            public void run() {
                ((StructuredViewer) viewer).update(getResourceInPythonModel(resource), null);
            }
        };
    }

}
