package org.python.pydev.ast.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

public class OperationsOnAllAddedFiles {

    /**
     * We must mark .pyc files as derived.
     * @param resource the resource to be marked as derived.
     */
    public static void markAsDerived(IResource resource) {
        try {
            resource.setDerived(true, new NullProgressMonitor());
        } catch (CoreException e) {
            // Log.log(e); -- being deleted in the meanwhile is expected.
        }
    }

    /**
     * @return true if we should skip further processing in this file and false
     * if it should proceed as usual.
     */
    public static boolean checkDerived(IResource resource) {
        IPath fullPath = resource.getFullPath();
        if (fullPath == null) {
            return false;
        }
        int segmentCount = fullPath.segmentCount();
        for (int i = 0; i < segmentCount; i++) {
            String segment = fullPath.segment(i);
            if (".mypy_cache".equals(segment) || ".pytest_cache".equals(segment)) {
                markAsDerived(resource);
                return true;
            }
        }
        return false;
    }

}
