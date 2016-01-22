/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This class wraps a java project as we'd wrap a python project in a ProjectModulesManager, to give info on the
 * modules available.
 *
 * Alternative to find the package names:
 *             SearchableEnvironment s = j.newSearchableNameEnvironment(new ICompilationUnit[]{unit});
 *             s.findPackages("bar".toCharArray(), new ISearchRequestor(){
 *
 *                 public void acceptPackage(char[] packageName) {
 *                     System.out.println("Accept package:"+new String(packageName));
 *                 }
 *
 *                 public void acceptType(char[] packageName, char[] typeName, char[][] enclosingTypeNames, int modifiers,
 *                         AccessRestriction accessRestriction) {
 *                     System.out.println("Accept type:"+new String(packageName)+" / "+new String(typeName));
 *                 }});
 * End Alternative
 *
 * Message about it: http://www.eclipse.org/newsportal/article.php?id=21742&group=eclipse.tools.jdt#21742
 *
 * @author Fabio
 */
public class JavaProjectModulesManager implements IModulesManager, IProjectModulesManager {

    private static final String[] EMPTY_STRINTG_ARRAY = new String[0];

    // DEBUG CONSTANTS
    private static final boolean DEBUG_GET_MODULE = false;

    private static final boolean DEBUG_GET_DIRECT_MODULES = false;

    private IJavaProject javaProject;

    public JavaProjectModulesManager(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    /**
     * @return a map with the modules keys for all the available modules that start with the passed token.
     */
    public SortedMap<ModulesKey, ModulesKey> getAllDirectModulesStartingWith(final String moduleToGetTokensFrom) {
        if (DEBUG_GET_DIRECT_MODULES) {
            System.out.println("getAllDirectModulesStartingWith: " + moduleToGetTokensFrom);
        }
        final TreeMap<ModulesKey, ModulesKey> ret = new TreeMap<ModulesKey, ModulesKey>();

        filterJavaPackages(new IFilter() {

            public boolean accept(String elementName, IPackageFragmentRoot packageRoot, IJavaElement javaElement) {
                if (elementName.startsWith(moduleToGetTokensFrom) && elementName.length() > 0) { //we don't want the 'default' package here!
                    if (DEBUG_GET_DIRECT_MODULES) {
                        System.out.println("getAllDirectModulesStartingWith: found:" + elementName);
                    }

                    ModulesKeyForJava key = new ModulesKeyForJava(elementName, packageRoot, javaElement);
                    ret.put(key, key);

                    //as we care about the full module name here, we'll only try to check the classes if
                    //the package name already starts with what we're looking for...
                    return true;
                }

                if (elementName.length() == 0) {
                    //or if we're in the default package.
                    return true;
                }

                return false;
            }

        });

        return ret;
    }

    /**
     * @return a set with all the module names contained in this modules manager (only in this modules manager,
     * as the addDependencies should never be true in this implementation).
     */
    public Set<String> getAllModuleNames(boolean addDependencies, final String partStartingWithLowerCase) {
        if (addDependencies) {
            throw new RuntimeException("At this point, it should never be called with dependencies "
                    + "(because it's a java project already -- it manages that internally already)");
        }

        final HashSet<String> ret = new HashSet<String>();

        filterJavaPackages(new IFilter() {

            public boolean accept(String elementName, IPackageFragmentRoot packageRoot, IJavaElement javaElement) {
                for (String mod : StringUtils.dotSplit(elementName)) {
                    if (mod.toLowerCase().startsWith(partStartingWithLowerCase)) {
                        ret.add(elementName);
                    }
                }
                return true;
            }

        });
        return ret;
    }

    /**
     * Interface to be passed to filter a java package.
     *
     * @author Fabio
     */
    public static interface IFilter {
        /**
         * @param elementName the name of the element (same as javaElement.getElementName())
         * @param packageRoot the java package where the element is contained
         * @param javaElement the java element
         *
         * @return true if the element should be added and false otherwise.
         */
        public boolean accept(String elementName, IPackageFragmentRoot packageRoot, IJavaElement javaElement);
    }

    /**
     * This method passes through all the java packages and calls the filter callback passed
     * on each package found.
     *
     * If true is returned on the callback, the children of each package (classes) will also be visited,
     * otherwise, they'll be skipped.
     */
    private void filterJavaPackages(IFilter filter) {
        IClasspathEntry[] rawClasspath;
        try {
            rawClasspath = this.javaProject.getRawClasspath();
            FastStringBuffer buffer = new FastStringBuffer();
            for (IClasspathEntry entry : rawClasspath) {
                int entryKind = entry.getEntryKind();
                IClasspathEntry resolvedClasspathEntry = JavaCore.getResolvedClasspathEntry(entry);
                if (entryKind != IClasspathEntry.CPE_CONTAINER) {
                    //ignore if it's in the system classpath...
                    IPackageFragmentRoot[] roots = javaProject.findPackageFragmentRoots(resolvedClasspathEntry);

                    //get the package roots
                    for (IPackageFragmentRoot root : roots) {
                        IJavaElement[] children = root.getChildren();

                        //get the actual packages
                        for (IJavaElement child : children) {
                            IPackageFragment childPackage = (IPackageFragment) child;
                            String elementName = childPackage.getElementName();

                            //and if the java package is 'accepted'
                            if (filter.accept(elementName, root, childPackage)) {
                                buffer.clear();
                                buffer.append(elementName);
                                int packageNameLen = buffer.length();
                                if (packageNameLen > 0) {
                                    buffer.append('.');
                                    packageNameLen++;
                                }

                                //traverse its classes
                                for (IJavaElement class_ : childPackage.getChildren()) {
                                    buffer.append(FullRepIterable.getFirstPart(class_.getElementName()));
                                    filter.accept(buffer.toString(), root, class_);
                                    buffer.setCount(packageNameLen); //leave only the package part for the next append
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getBuiltins() {
        return EMPTY_STRINTG_ARRAY;
    }

    public List<String> getCompletePythonPath(IInterpreterInfo interpreter, IInterpreterManager manager) {
        return new ArrayList<String>();
    }

    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        return this.getModuleInDirectManager(name, nature, dontSearchInit);
    }

    public IModule getModule(String name, IPythonNature nature, boolean checkSystemManager, boolean dontSearchInit) {
        return this.getModuleInDirectManager(name, nature, dontSearchInit);
    }

    public IPythonNature getNature() {
        return null;
    }

    public boolean hasModule(ModulesKey key) {
        return false;
    }

    public ModulesKey[] getOnlyDirectModules() {
        return new ModulesKey[0];
    }

    public Object getPythonPathHelper() {
        return null;
    }

    public void setPythonPathHelper(Object helper) {
        return; // noop
    }

    public IModule getRelativeModule(String name, IPythonNature nature) {
        return this.getModuleInDirectManager(name, nature, true);
    }

    public int getSize(boolean addDependenciesSize) {
        return 0;
    }

    public ISystemModulesManager getSystemModulesManager() {
        return null;
    }

    public Tuple<IModule, IModulesManager> getModuleAndRelatedModulesManager(String name, IPythonNature nature,
            boolean checkSystemManager, boolean dontSearchInit) {
        IModule module = this.getModule(name, nature, checkSystemManager, dontSearchInit);
        if (module != null) {
            return new Tuple<IModule, IModulesManager>(module, this);
        }
        return null;
    }

    /**
     * @param dontSearchInit: not applicable for this method (ignored)
     * @return the module that corresponds to the passed name.
     */
    public IModule getModuleInDirectManager(String name, IPythonNature nature, boolean dontSearchInit) {
        if (DEBUG_GET_MODULE) {
            System.out.println("Trying to get module in java project modules manager: " + name);
        }
        if (name.startsWith(".")) { //this happens when looking for a relative import
            return null;
        }
        try {
            IJavaElement javaElement = this.javaProject.findType(name);
            if (javaElement == null) {
                javaElement = this.javaProject.findElement(new Path(name.replace('.', '/')));
            }

            if (DEBUG_GET_MODULE) {
                System.out.println("Found: " + javaElement);
            }

            if (javaElement != null) {

                //now, there's a catch here, we'll find any class in the project classpath, even if it's in the
                //global classpath (e.g.: rt.jar), and this shouldn't be treated in this project modules manager
                //(that's treated in the Jython system manager)
                IJavaElement ancestor = javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
                if (ancestor instanceof IPackageFragmentRoot) {
                    IPackageFragmentRoot packageFragmentRoot = (IPackageFragmentRoot) ancestor;
                    IClasspathEntry rawClasspathEntry = packageFragmentRoot.getRawClasspathEntry();
                    if (rawClasspathEntry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                        return null;
                    }
                }
                return new JavaModuleInProject(name, this.javaProject);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public String resolveModuleInDirectManager(IFile file) {
        return null;
    }

    public String resolveModuleInDirectManager(String full) {
        return null;
    }

    //------------------------------------------------------------------------------------------------------------------
    //the methods below are not actually implemented for a java project (as they aren't really applicable)
    //------------------------------------------------------------------------------------------------------------------

    public boolean isInPythonPath(IResource member, IProject container) {
        throw new RuntimeException("Not implemented");
    }

    public String resolveModule(IResource member, IProject container) {
        throw new RuntimeException("Not implemented");
    }

    public String resolveModule(String full) {
        throw new RuntimeException("Not implemented");
    }

    public String resolveModule(String full, boolean checkSystemManager) {
        throw new RuntimeException("Not implemented");
    }

    public void setPythonNature(IPythonNature nature) {
        throw new RuntimeException("Not implemented");
    }

    public boolean startCompletionCache() {
        throw new RuntimeException("Not implemented");
    }

    public void endCompletionCache() {
        throw new RuntimeException("Not implemented");
    }

    public void endProcessing() {
        throw new RuntimeException("Not implemented");
    }

    public SortedMap<ModulesKey, ModulesKey> getAllModulesStartingWith(String moduleToGetTokensFrom) {
        throw new RuntimeException("Not implemented"); //should never be called (this modules manager is inside another one that should handle it)
    }

    public IModule addModule(ModulesKey key) {
        throw new RuntimeException("Not implemented");
    }

    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    public void removeModules(Collection<ModulesKey> toRem) {
        throw new RuntimeException("Not implemented");
    }

    public void processDelete(ModulesKey key) {
        throw new RuntimeException("Not implemented");
    }

    public void processInsert(ModulesKey key) {
        throw new RuntimeException("Not implemented");
    }

    public void processUpdate(ModulesKey data) {
        throw new RuntimeException("Not implemented");
    }

    public void rebuildModule(File f, ICallback0<IDocument> doc, IProject project, IProgressMonitor monitor,
            IPythonNature nature) {
        throw new RuntimeException("Not implemented");
    }

    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    public void setProject(IProject project, IPythonNature nature, boolean restoreDeltas) {
        throw new RuntimeException("Not implemented");
    }

    public int pushTemporaryModule(String moduleName, IModule module) {
        throw new RuntimeException("Not implemented");
    }

    public void popTemporaryModule(String moduleName, int handle) {
        throw new RuntimeException("Not implemented");
    }

    public void saveToFile(File workspaceMetadataFile) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public AutoCloseable withNoGenerateDeltas() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Object getCompiledModuleCreationLock(String name) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Tuple<List<ModulesKey>, List<ModulesKey>> diffModules(AbstractMap<ModulesKey, ModulesKey> keysFound) {
        throw new RuntimeException("not implemented");
    }
}
