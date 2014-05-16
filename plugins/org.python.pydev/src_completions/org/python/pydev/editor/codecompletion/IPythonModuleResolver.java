/*******************************************************************************
 * Copyright (c) 2014 Google, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Danny Yoo (Google) - initial API and implementation
 *******************************************************************************/
package org.python.pydev.editor.codecompletion;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Allows third-party plugins to provide an alternative method for Python module discovery and
 * resolution.
 */
public interface IPythonModuleResolver {
    /**
     * Resolves a module's absolute file location to its dot-separated qualified name.  The file at
     * the given location does not have to exist.
     * <p>
     * The returned dot-separated qualified name should be interpretable as a relative path to one
     * of the given base locations.  Implementations might deviate from this rule to simulate a
     * Python import hook.
     *
     * @param project the project context in which the module resolution is being performed.
     *      Can be {@code null} if resolution should be done for the workspace instead
     *      of a particular project.
     * @param moduleLocation the absolute file system location of the module.
     *      Only for directories, or .py, .pyd, .dll, .so, .pyo files.
     * @param baseLocations the locations relative to which to resolve the Python module.
     * @return the qualified name of the module. e.g. {@code compiler.ast}.
     *      If the module can not be resolved, returns an empty string.
     *      A {@code null} returned from the method means that the module resolution has to be
     *      delegated to the next module resolver or to the default PyDev module resolution.
     */
    String resolveModule(IProject project, IPath moduleLocation, List<IPath> baseLocations);

    /**
     * Collects all the Python modules, including directories, files and zip packages, in a project
     * or the entire workspace.
     *
     * @param project the project to collect Python modules for, or {@code null} to indicate
     *      an interpreter-wide collection.
     * @param monitor the progress monitor.  Can be {@code null}.
     * @return a collection of the found Python modules and zip files as absolute file locations.
     *      Each module is expected to be resolvable with
     *      {@link #resolveModule(IProject, IPath, List)}.
     *      A {@code null} returned from the method means that the module discovery has to be
     *      delegated to the next module resolver or to the default PyDev module resolution.
     */
    Collection<IPath> findAllModules(IProject project, IProgressMonitor monitor);
}