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
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.FileUtilsFileBuffer;
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

        if (this.isFullBuild()) {
            //Don't check if it's an __init__ (in which case we'd get sub-modules) unless we're doing a delta build
            //(i.e.: on a full build we'll visit those anyways).
            return;
        }
        if (PythonPathHelper.isValidInitFile(resource.getName())) {
            //When some init file is added, we have to visit the whole structure below it as we'll have to enable
            //code-completion for all of that!
            Long originalTime = (Long) memo.get(PyDevBuilderVisitor.DOCUMENT_TIME);
            try {
                IResource[] initDependents = getInitDependents(resource);
                int length = initDependents.length;
                for (int i = 0; i < length; i++) {
                    IResource dependent = initDependents[i];
                    memo.put(PyDevBuilderVisitor.DOCUMENT_TIME, System.currentTimeMillis());
                    this.visitChangedResource(dependent, FileUtilsFileBuffer.getDocOnCallbackFromResource(dependent),
                            monitor);
                }
            } finally {
                memo.put(PyDevBuilderVisitor.DOCUMENT_TIME, originalTime);
            }
        }
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

    /**
     * @return all the IFiles that are below the folder where initResource is located.
     */
    protected IResource[] getInitDependents(IResource initResource) {

        List<IResource> toRet = new ArrayList<IResource>();
        IContainer parent = initResource.getParent();

        try {
            //Remove the __init__ that originated this request.
            IResource[] members = parent.members();
            ArrayList<IResource> lst = new ArrayList<IResource>(members.length - 1);
            for (int i = 0; i < members.length; i++) {
                IResource resource = members[i];
                if (!PythonPathHelper.isValidInitFile(resource.getName())) {
                    lst.add(resource);
                }
            }

            fillWithMembers(toRet, parent, lst.toArray(new IResource[lst.size()]));
        } catch (CoreException e) {
            //That's OK: it may not exist anymore.
        }
        return toRet.toArray(new IResource[0]);
    }

    private void fillWithMembers(List<IResource> toRet, IContainer parent, IResource[] resources) {
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            if (resource.getType() == IResource.FILE) {
                if (PythonPathHelper.isValidSourceFile(resource.getName())) {
                    toRet.add(resource);
                }

            } else if (resource.getType() == IResource.FOLDER) {
                IFolder folder = (IFolder) resource;
                IResource[] members;
                try {
                    members = folder.members();
                    for (int j = 0; j < members.length; j++) {
                        if (PythonPathHelper.isValidInitFile(members[j].getName())) {
                            fillWithMembers(toRet, folder, members);
                            break;
                        }
                    }
                } catch (CoreException e) {
                    //That's OK: it may not exist anymore.
                }
            }
        }
    }

}