/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.changes;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.docutils.StringUtils;

/**
 * This action is able to do a rename / move for some python module.
 */
public final class PyRenameResourceChange extends PyChange {

    public static IPath renamedResourcePath(IPath path, String newName) {
        return path.removeLastSegments(1).append(newName);
    }

    private final String fComment;

    private final String fNewName;

    private final IPath fResourcePath;

    private final long fStampToRestore;

    private final String fInitialName;

    private PyRenameResourceChange(IPath resourcePath, String initialName, String newName, String comment,
            long stampToRestore) {
        fResourcePath = resourcePath;
        fNewName = newName;
        fInitialName = initialName;
        fComment = comment;
        fStampToRestore = stampToRestore;
    }

    public PyRenameResourceChange(IResource resource, String initialName, String newName, String comment) {
        this(resource.getFullPath(), initialName, newName, comment, IResource.NULL_STAMP);
    }

    @Override
    public Object getModifiedElement() {
        return getResource();
    }

    @Override
    public String getName() {
        return org.python.pydev.shared_core.string.StringUtils.format("Change %s to %s", fInitialName, fNewName);
    }

    public String getNewName() {
        return fNewName;
    }

    private IResource getResource() {
        return ResourcesPlugin.getWorkspace().getRoot().findMember(fResourcePath);
    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
        IResource resource = getResource();
        if (resource == null || !resource.exists()) {
            return RefactoringStatus.createFatalErrorStatus(org.python.pydev.shared_core.string.StringUtils.format(
                    "Resource %s does not exist",
                    fResourcePath));
        } else {
            return super.isValid(pm, DIRTY);
        }
    }

    @Override
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

            //The undo command
            return new PyRenameResourceChange(newPath, fNewName, fInitialName, fComment, currentStamp);
        } finally {
            pm.done();
        }
    }

    //    @Override
    //    Change doPerformReorg(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    //        // get current modification stamp
    //        long currentStamp = IResource.NULL_STAMP;
    //        IResource resource = getResource();
    //        if (resource != null) {
    //            currentStamp = resource.getModificationStamp();
    //        }
    //
    //        IContainer destination = createDestination(resource, pm);
    //
    //        if (!destination.exists()) {
    //            StringUtils.split(fInitialName, ".");
    //        }
    //
    //        // perform the move and restore modification stamp
    //        getCu().move(destination, null, newName, true, pm);
    //        if (fStampToRestore != IResource.NULL_STAMP) {
    //            ICompilationUnit moved = destination.getCompilationUnit(name);
    //            IResource movedResource = moved.getResource();
    //            if (movedResource != null) {
    //                movedResource.revertModificationStamp(fStampToRestore);
    //            }
    //        }
    //
    //        if (fDeletePackages != null) {
    //            for (int i = fDeletePackages.length - 1; i >= 0; i--) {
    //                fDeletePackages[i].delete(true, pm);
    //            }
    //        }
    //
    //        if (fUndoable) {
    //            return new MoveCompilationUnitChange(destination, getCu().getElementName(), getOldPackage(), currentStamp,
    //                    createdPackages);
    //        } else {
    //            return null;
    //        }
    //    }

    //    private IContainer getDestinationFolder() {
    //
    //    }

    /**
     * Returns the final folder for the created module and the resources created in the process. 
     * 
     * Receives the resource (i.e.: in filesystem), the resolved name (i.e.: my.mod1) and the final name (i.e.: bar.foo).
     */
    public static IContainer getDestination(IResource initialResource, String initialName, String finalName,
            IProgressMonitor pm) {
        List<String> initialParts = StringUtils.split(initialName, ".");
        List<String> finalParts = StringUtils.split(finalName, ".");

        int startFrom = 0;
        int finalPartSize = finalParts.size();
        int initialPartSize = initialParts.size();

        initialPartSize--;
        String initialNamePart = initialParts.remove(initialPartSize); //remove the last, as that's the name

        finalPartSize--;
        String finalNamePart = finalParts.remove(finalPartSize); //remove the last, as that's the name

        //Get variable startFrom to the first place where the parts differ.
        for (; startFrom < finalPartSize; startFrom++) {
            String part = finalParts.get(startFrom);
            if (startFrom < initialPartSize) {
                String initial = initialParts.get(startFrom);
                if (!initial.equals(part)) {
                    break;
                }
            } else {
                break;
            }
        }

        List<String> createParts = finalParts.subList(startFrom, finalPartSize); //the last path is the file, not the folder, so, skip it.
        List<String> backtrackParts = initialParts.subList(startFrom, initialPartSize);
        Collections.reverse(backtrackParts);
        IResource resource = initialResource;
        IContainer container = resource.getParent(); //always start from our container.
        for (String string : backtrackParts) {
            container = container.getParent();
        }

        if (createParts.size() > 0) {
            container = container.getFolder(new Path(StringUtils.join("/", createParts)));
        }

        return container;
    }
}
