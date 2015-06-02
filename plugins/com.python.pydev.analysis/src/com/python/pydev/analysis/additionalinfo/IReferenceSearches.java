package com.python.pydev.analysis.additionalinfo;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.python.pydev.core.ModulesKey;

public interface IReferenceSearches {

    List<ModulesKey> search(IProject project, String token, IProgressMonitor monitor) throws OperationCanceledException;

    void dispose();

}
