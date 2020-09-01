package org.python.pydev.ast.builder;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.core.log.Log;

public class OperationsOnAllAddedFiles {

    /**
     * We must mark .pyc files as derived.
     * @param resource the resource to be marked as derived.
     */
    public static void markAsDerived(IResource resource) {
        try {
            resource.setDerived(true);
        } catch (CoreException e) {
            Log.log(e);
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
            if (".mypy_cache".equals(fullPath.segment(i))) {
                markAsDerived(resource);
                return true;
            }
        }
        return false;
    }

}
