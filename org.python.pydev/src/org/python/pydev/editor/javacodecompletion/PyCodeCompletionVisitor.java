/*
 * Created on Nov 10, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;

/**
 * @author Fabio Zadrozny
 * 
 * This class
 */
public class PyCodeCompletionVisitor extends PyDevBuilderVisitor {

    /**
     * The code completion visitor is responsible for checking the changed resources in order to
     * update the code completion cache for the project. 
     * 
     * This visitor just passes one resource and updates the code completion cache for it.
     * 
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public boolean visitResource(IResource resource, IDocument document) {

//        IProject project = resource.getProject();
//        
//        long modificationStamp = resource.getModificationStamp();
//        
//        /* this is the object location - it is needed because we need to determine
//         * the file module. We should not allow overlapping modules!! 
//         * 
//         * If we do, this must be treated later. For now, a file should only represent one module.
//         * 
//         * Note: Some modules might seem overlapping if gotten from python
//         * 
//         * E.g.:
//         * 
//		 * 'C:\\bin\\Python23\\lib', 
//		 * 'C:\\bin\\Python23\\lib\\site-packages', 
//         * 'C:\\bin\\Python23\\lib\\site-packages\\Pythonwin',
//         * 'C:\\bin\\Python23\\lib\\plat-win', 
//         * 
//         * But actually they are not, as the site-packages and plat-win are not valid python tokens, and
//         * therefore cannot be used as modules. 
//         */
//        IPath location = resource.getLocation(); 
//
//        Object[] obj = PyParser.reparseDocument(document, true);
//        if (obj[0] != null && obj[0] instanceof SimpleNode) {
//            SimpleNode n = (SimpleNode) obj[0];
//        }

        return false;
    }

}