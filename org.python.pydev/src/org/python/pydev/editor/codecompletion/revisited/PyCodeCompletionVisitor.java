/*
 * Created on Nov 10, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
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
        
        IProject project = resource.getProject();
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        IASTManager astManager = pythonNature.getAstManager();
        
        if (astManager != null){
            IPath location = resource.getLocation(); 
            astManager.rebuildModule(new File(location.toOSString()), document, resource.getProject(), new NullProgressMonitor(), pythonNature);
        }

        return false;
    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public boolean visitRemovedResource(IResource resource, IDocument document) {

        IProject project = resource.getProject();
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        IASTManager astManager = pythonNature.getAstManager();
        
        if (astManager != null){
            IPath location = resource.getLocation(); 

            if(resource.getType() == IResource.FOLDER){
                astManager.removeModulesBelow(new File(location.toOSString()), resource.getProject(), new NullProgressMonitor());
            }else{
	            astManager.removeModule(new File(location.toOSString()), resource.getProject(), new NullProgressMonitor());
            }
        }

        return false;
    }

}