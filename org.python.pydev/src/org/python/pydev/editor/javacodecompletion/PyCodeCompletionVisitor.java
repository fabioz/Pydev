/*
 * Created on Nov 10, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.python.parser.SimpleNode;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.parser.PyParser;

/**
 * @author Fabio Zadrozny
 * 
 * This class
 */
public class PyCodeCompletionVisitor extends PyDevBuilderVisitor {

    public boolean visitResource(IResource resource, IDocument document) {

        long modificationStamp = resource.getModificationStamp();
        
        /* this is the object location - it is needed because we need to determine
         * the file module. We should not allow overlapping modules!! 
         * 
         * If we do, this must be treated later. For now, a file should only represent one module.
         * 
         * Note: Some modules might seem overlapping if gotten from python
         * 
         * E.g.:
         * 
		 * 'C:\\bin\\Python23\\lib', 
		 * 'C:\\bin\\Python23\\lib\\site-packages', 
         * 'C:\\bin\\Python23\\lib\\site-packages\\Pythonwin',
         * 'C:\\bin\\Python23\\lib\\plat-win', 
         * 
         * But actually they are not, as the site-packages and plat-win are not valid python tokens, and
         * therefore cannot be used as modules. 
         */
        IPath location = resource.getLocation(); 

        Object[] obj = PyParser.reparseDocument(document, true);
        if (obj[0] != null && obj[0] instanceof SimpleNode) {
            SimpleNode n = (SimpleNode) obj[0];
        }

        return false;
    }

}