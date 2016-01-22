/**
 * Copyright 2005-2013 Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Modifications Copyright(c) 2014 Google, Inc.
 */
package org.python.pydev.shared_ui.search.replace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;

/**
 * Checks changed files with a workspace's {@link IWorkspace#validateEdit(IFile[], Object)}
 * to integrate with a VCS.
 */
public class ChangedFilesChecker {
    /**
     * Checks the given files that have been changed by validating them with the workspace.
     * 
     * @param files the files to check
     * @param validationContext the context for validating the files.  Should be the value of 
     *      {@link org.eclipse.ltk.core.refactoring.Refactoring#getValidationContext()}.
     * @param refactoringStatus the value to store the detection of problems.
     * @throws CoreException when the validation is canceled
     */
    public static void checkFiles(Collection<IFile> files, Object validationContext,
            RefactoringStatus refactoringStatus) throws CoreException {
        List<IFile> readOnly = new ArrayList<IFile>();
        for (IFile file : files) {
            if (file.isReadOnly()) {
                readOnly.add(file);
            }
        }
        if (ResourcesPlugin.getPlugin() == null) {
            //i.e.: in test mode we won't be able to get the workspace
            return;
        }
        if (!readOnly.isEmpty()) {
            IFile[] readOnlyFiles = readOnly.toArray(new IFile[readOnly.size()]);
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IStatus status = workspace.validateEdit(readOnlyFiles, validationContext);
            if (status.getSeverity() == IStatus.CANCEL) {
                throw new OperationCanceledException();
            }
            refactoringStatus.merge(RefactoringStatus.create(status));
            if (refactoringStatus.hasFatalError()) {
                return;
            }
        }
        refactoringStatus.merge(ResourceChangeChecker.checkFilesToBeChanged(
                files.toArray(new IFile[files.size()]), null));
    }
}
