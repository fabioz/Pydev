/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.changes;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Largely gotten from JDTChange
 */
public abstract class PyChange extends Change {

    private long fModificationStamp;
    private boolean fReadOnly;

    private static class ValidationState {
        private IResource fResource;
        private int fKind;
        private boolean fDirty;
        private boolean fReadOnly;
        private long fModificationStamp;
        private ITextFileBuffer fTextFileBuffer;
        public static final int RESOURCE = 1;
        public static final int DOCUMENT = 2;

        public ValidationState(IResource resource) {
            fResource = resource;
            if (resource instanceof IFile) {
                initializeFile((IFile) resource);
            } else {
                initializeResource(resource);
            }
        }

        public void checkDirty(RefactoringStatus status, long stampToMatch, IProgressMonitor pm) throws CoreException {
            if (fDirty) {
                if (fKind == DOCUMENT && fTextFileBuffer != null && stampToMatch == fModificationStamp) {
                    fTextFileBuffer.commit(pm, false);
                } else {
                    status.addFatalError(StringUtils.format("Resource %s is unsaved", fResource.getFullPath()));
                }
            }
        }

        public void checkDirty(RefactoringStatus status) {
            if (fDirty) {
                status.addFatalError(StringUtils.format("Resource %s is unsaved", fResource.getFullPath()));
            }
        }

        public void checkReadOnly(RefactoringStatus status) {
            if (fReadOnly) {
                status.addFatalError(StringUtils.format("Resource %s is read-only", fResource.getFullPath()));
            }
        }

        public void checkSameReadOnly(RefactoringStatus status, boolean valueToMatch) {
            if (fReadOnly != valueToMatch) {
                status.addFatalError(StringUtils.format("Resource %s (Change_same_read_only)", fResource.getFullPath()));
            }
        }

        public void checkModificationStamp(RefactoringStatus status, long stampToMatch) {
            if (fKind == DOCUMENT) {
                if (stampToMatch != IDocumentExtension4.UNKNOWN_MODIFICATION_STAMP
                        && fModificationStamp != stampToMatch) {
                    status.addFatalError(StringUtils.format("Resource %s has modifications", fResource.getFullPath()));
                }
            } else {
                if (stampToMatch != IResource.NULL_STAMP && fModificationStamp != stampToMatch) {
                    status.addFatalError(StringUtils.format("Resource %s has modifications", fResource.getFullPath()));

                }
            }
        }

        private void initializeFile(IFile file) {
            fTextFileBuffer = getBuffer(file);
            if (fTextFileBuffer == null) {
                initializeResource(file);
            } else {
                IDocument document = fTextFileBuffer.getDocument();
                fDirty = fTextFileBuffer.isDirty();
                fReadOnly = isReadOnly(file);
                if (document instanceof IDocumentExtension4) {
                    fKind = DOCUMENT;
                    fModificationStamp = ((IDocumentExtension4) document).getModificationStamp();
                } else {
                    fKind = RESOURCE;
                    fModificationStamp = file.getModificationStamp();
                }
            }

        }

        private void initializeResource(IResource resource) {
            fKind = RESOURCE;
            fDirty = false;
            fReadOnly = isReadOnly(resource);
            fModificationStamp = resource.getModificationStamp();
        }
    }

    public static boolean isReadOnly(IResource resource) {
        ResourceAttributes resourceAttributes = resource.getResourceAttributes();
        if (resourceAttributes == null) {
            return false;
        }
        return resourceAttributes.isReadOnly();
    }

    protected static final int NONE = 0;
    protected static final int READ_ONLY = 1 << 0;
    protected static final int DIRTY = 1 << 1;
    private static final int SAVE = 1 << 2;
    protected static final int SAVE_IF_DIRTY = SAVE | DIRTY;

    protected PyChange() {
        fModificationStamp = IResource.NULL_STAMP;
        fReadOnly = false;
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm) {
        IResource resource = getResource(getModifiedElement());
        if (resource != null) {
            fModificationStamp = getModificationStamp(resource);
            fReadOnly = isReadOnly(resource);
        }
    }

    // protected final RefactoringStatus isValid(IProgressMonitor pm, boolean checkReadOnly, boolean checkDirty) throws CoreException {
    protected final RefactoringStatus isValid(IProgressMonitor pm, int flags) throws CoreException {
        pm.beginTask("", 2); //$NON-NLS-1$
        try {
            RefactoringStatus result = new RefactoringStatus();
            Object modifiedElement = getModifiedElement();
            checkExistence(result, modifiedElement);
            if (result.hasFatalError()) {
                return result;
            }
            if (flags == NONE) {
                return result;
            }
            IResource resource = getResource(modifiedElement);
            if (resource != null) {
                ValidationState state = new ValidationState(resource);
                state.checkModificationStamp(result, fModificationStamp);
                if (result.hasFatalError()) {
                    return result;
                }
                state.checkSameReadOnly(result, fReadOnly);
                if (result.hasFatalError()) {
                    return result;
                }
                if ((flags & READ_ONLY) != 0) {
                    state.checkReadOnly(result);
                    if (result.hasFatalError()) {
                        return result;
                    }
                }
                if ((flags & DIRTY) != 0) {
                    if ((flags & SAVE) != 0) {
                        state.checkDirty(result, fModificationStamp, new SubProgressMonitor(pm, 1));
                    } else {
                        state.checkDirty(result);
                    }
                }
            }
            return result;
        } finally {
            pm.done();
        }
    }

    protected final RefactoringStatus isValid(int flags) throws CoreException {
        return isValid(new NullProgressMonitor(), flags);
    }

    protected static void checkIfModifiable(RefactoringStatus status, Object element, int flags) {
        checkIfModifiable(status, getResource(element), flags);
    }

    protected static void checkIfModifiable(RefactoringStatus result, IResource resource, int flags) {
        checkExistence(result, resource);
        if (result.hasFatalError()) {
            return;
        }
        if (flags == NONE) {
            return;
        }
        ValidationState state = new ValidationState(resource);
        if ((flags & READ_ONLY) != 0) {
            state.checkReadOnly(result);
            if (result.hasFatalError()) {
                return;
            }
        }
        if ((flags & DIRTY) != 0) {
            state.checkDirty(result);
        }
    }

    protected static void checkExistence(RefactoringStatus status, Object element) {
        if (element == null) {
            status.addFatalError("Workspace Changed");

        } else if (element instanceof IResource && !((IResource) element).exists()) {
            status.addFatalError(StringUtils.format("Resource %s does not exist", ((IResource) element).getFullPath()
                    .toString()));
        }
    }

    private static IResource getResource(Object element) {
        if (element instanceof IResource) {
            return (IResource) element;
        }
        if (element instanceof IAdaptable) {
            return (IResource) ((IAdaptable) element).getAdapter(IResource.class);
        }
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    public long getModificationStamp(IResource resource) {
        if (!(resource instanceof IFile)) {
            return resource.getModificationStamp();
        }
        IFile file = (IFile) resource;
        ITextFileBuffer buffer = getBuffer(file);
        if (buffer == null) {
            return file.getModificationStamp();
        } else {
            IDocument document = buffer.getDocument();
            if (document instanceof IDocumentExtension4) {
                return ((IDocumentExtension4) document).getModificationStamp();
            } else {
                return file.getModificationStamp();
            }
        }
    }

    private static ITextFileBuffer getBuffer(IFile file) {
        try {
            ITextFileBufferManager manager = ITextFileBufferManager.DEFAULT;
            return manager.getTextFileBuffer(file.getFullPath(), org.eclipse.core.filebuffers.LocationKind.IFILE);
        } catch (Throwable e) {//NoSuchMethod/NoClassDef exception 
            if (e instanceof ClassNotFoundException || e instanceof LinkageError || e instanceof NoSuchMethodException
                    || e instanceof NoSuchMethodError || e instanceof NoClassDefFoundError) {
                return null; // that's ok -- not available in Eclipse 3.2
            }
            throw new RuntimeException(e);
        }
    }
}
