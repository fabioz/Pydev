/*
 * Created on Nov 10, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.plugin.PythonNature;

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

        System.out.println("Visiting delta");
        IProject project = resource.getProject();
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        pythonNature.rebuildDelta(resource, document);

        return false;
    }

}