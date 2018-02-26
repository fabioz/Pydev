/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 10, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.revisited;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.builder.PyDevBuilderVisitor;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback0;

/**
 * @author Fabio Zadrozny
 *
 * This class updates our internal code-completion related structures.
 */
public class PyCodeCompletionVisitor extends PyDevBuilderVisitor {

    public static final int PRIORITY_CODE_COMPLETION = PRIORITY_DEFAULT;
    private AutoCloseable noGenerateDeltas;

    @Override
    protected int getPriority() {
        return PRIORITY_CODE_COMPLETION;
    }

    /**
     * On a full build we'll stop generating deltas (the build is much faster this way).
     */
    @Override
    public void visitingWillStart(IProgressMonitor monitor, boolean isFullBuild, IPythonNature nature) {
        if (isFullBuild) {
            ICodeCompletionASTManager astManager = nature.getAstManager();
            if (astManager != null) {
                IModulesManager modulesManager = astManager.getModulesManager();
                noGenerateDeltas = modulesManager.withNoGenerateDeltas();
            }
        }
    }

    @Override
    public void visitingEnded(IProgressMonitor monitor) {
        if (noGenerateDeltas != null) {
            try {
                noGenerateDeltas.close();
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * The code completion visitor is responsible for checking the changed resources in order to
     * update the code completion cache for the project.
     *
     * This visitor just passes one resource and updates the code completion cache for it.
     */
    @Override
    public void visitChangedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        PythonNature pythonNature = getPythonNature(resource);
        if (pythonNature != null) {
            ICodeCompletionASTManager astManager = pythonNature.getAstManager();

            if (astManager != null) {
                IPath location = resource.getLocation();
                astManager.rebuildModule(new File(location.toOSString()), document, resource.getProject(),
                        new NullProgressMonitor(), pythonNature);
            }
        }
    }

    @Override
    public void visitAddedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        visitChangedResource(resource, document, monitor);
        // Note: no longer check for __init__ (python 3.6 does no longer require __init__.py files).
    }

    @Override
    public void visitRemovedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        PythonNature pythonNature = getPythonNature(resource);
        if (pythonNature != null) {

            ICodeCompletionASTManager astManager = pythonNature.getAstManager();
            if (astManager != null) {
                IPath location = resource.getLocation();

                astManager.removeModule(new File(location.toOSString()), resource.getProject(),
                        new NullProgressMonitor());
            }
        }
    }

}