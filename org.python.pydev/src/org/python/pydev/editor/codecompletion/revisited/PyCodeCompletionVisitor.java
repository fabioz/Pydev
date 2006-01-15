/*
 * Created on Nov 10, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 * 
 * This class
 */
public class PyCodeCompletionVisitor extends PyDevBuilderVisitor {

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#shouldVisitInitDependency()
     */
    public boolean shouldVisitInitDependency() {
        return true;
    }
    
    /**
     * The code completion visitor is responsible for checking the changed resources in order to
     * update the code completion cache for the project. 
     * 
     * This visitor just passes one resource and updates the code completion cache for it.
     * 
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitChangedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        
        if(document != null){ //it might be out of sync...
            PythonNature pythonNature = getPythonNature(resource);
            if(pythonNature != null){
                ICodeCompletionASTManager astManager = pythonNature.getAstManager();
                
                if (astManager != null){
                    IPath location = resource.getLocation(); 
                    astManager.rebuildModule(new File(location.toOSString()), document, resource.getProject(), new NullProgressMonitor(), pythonNature);
                }
            }
        }

    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {

        PythonNature pythonNature = getPythonNature(resource);
        if(pythonNature != null){

            ICodeCompletionASTManager astManager = pythonNature.getAstManager();
            if (astManager != null){
                IPath location = resource.getLocation(); 
    
                astManager.removeModule(new File(location.toOSString()), resource.getProject(), new NullProgressMonitor());
            }
        }
    }

}