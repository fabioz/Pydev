/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.changes;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Largely gotten from org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange
 */
public final class PyRenameResourceChange extends PyChange {

    public static IPath renamedResourcePath(IPath path, String newName) {
        return path.removeLastSegments(1).append(newName);
    }

    private final String fComment;

    private final String fNewName;

    private final IPath fResourcePath;

    private final long fStampToRestore;

    private PyRenameResourceChange(IPath resourcePath, String newName, String comment, long stampToRestore) {
        fResourcePath = resourcePath;
        fNewName = newName;
        fComment = comment;
        fStampToRestore = stampToRestore;
    }

    public PyRenameResourceChange(IResource resource, String newName, String comment) {
        this(resource.getFullPath(), newName, comment, IResource.NULL_STAMP);
    }

    public Object getModifiedElement() {
        return getResource();
    }

    public String getName() {
        return org.python.pydev.shared_core.string.StringUtils.format("Rename %s to %s", fResourcePath, fNewName);
    }

    public String getNewName() {
        return fNewName;
    }

    private IResource getResource() {
        return ResourcesPlugin.getWorkspace().getRoot().findMember(fResourcePath);
    }

    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
        IResource resource = getResource();
        if (resource == null || !resource.exists()) {
            return RefactoringStatus.createFatalErrorStatus(org.python.pydev.shared_core.string.StringUtils.format("Resource %s does not exist",
                    fResourcePath));
        } else {
            return super.isValid(pm, DIRTY);
        }
    }

    public Change perform(IProgressMonitor pm) throws CoreException {
        try {
            pm.beginTask(getName(), 1);

            IResource resource = getResource();
            long currentStamp = resource.getModificationStamp();
            IPath newPath = renamedResourcePath(fResourcePath, fNewName);
            resource.move(newPath, IResource.SHALLOW, pm);
            if (fStampToRestore != IResource.NULL_STAMP) {
                IResource newResource = ResourcesPlugin.getWorkspace().getRoot().findMember(newPath);
                newResource.revertModificationStamp(fStampToRestore);
            }
            String oldName = fResourcePath.lastSegment();
            return new PyRenameResourceChange(newPath, oldName, fComment, currentStamp);
        } finally {
            pm.done();
        }
    }
}
