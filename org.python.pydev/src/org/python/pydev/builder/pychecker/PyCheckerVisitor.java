/*
 * Created on Oct 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pychecker;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.python.pydev.builder.PyDevBuilderVisitor;

/**
 * @author Fabio Zadrozny
 */
public class PyCheckerVisitor extends PyDevBuilderVisitor {

    /* (non-Javadoc)
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitResource(org.eclipse.core.resources.IResource)
     */
    public boolean visitResource(IResource resource) {
        if (PyCheckerPrefPage.usePyChecker() == false) {
            return false;
        }

//        PyCheckerLauncher.pycheck(resource);
        return true;
    }
    
    public void fullBuild(List resourcesToParse){
        if (PyCheckerPrefPage.usePyChecker() == false) {
            return;
        }
        
//        PyCheckerLauncher.pycheck(resourcesToParse);
    }
    
}
