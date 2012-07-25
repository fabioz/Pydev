/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.callbacks.ICallback0;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 * 
 * This class updates our internal code-completion related structures.
 */
public class PyCodeCompletionVisitor extends PyDevBuilderVisitor {

    public static final int PRIORITY_CODE_COMPLETION = PRIORITY_DEFAULT;

    @Override
    protected int getPriority() {
        return PRIORITY_CODE_COMPLETION;
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

        if (PythonPathHelper.isValidInitFile(resource.getName())) {
            //When some init file is added, we have to visit the whole structure below it as we'll have to enable
            //code-completion for all of that!
            Long originalTime = (Long) memo.get(PyDevBuilderVisitor.DOCUMENT_TIME);
            try {
                IResource[] initDependents = getInitDependents(resource);
                for (int i = 0; i < initDependents.length; i++) {
                    IResource dependent = initDependents[i];
                    memo.put(PyDevBuilderVisitor.DOCUMENT_TIME, System.currentTimeMillis());
                    this.visitChangedResource(dependent, REF.getDocOnCallbackFromResource(dependent), monitor);
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