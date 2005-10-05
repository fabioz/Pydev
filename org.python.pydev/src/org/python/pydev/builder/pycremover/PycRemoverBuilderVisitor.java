/*
 * Created on 14/09/2005
 */
package org.python.pydev.builder.pycremover;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.plugin.PydevPlugin;

public class PycRemoverBuilderVisitor extends PyDevBuilderVisitor{

    @Override
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    }

    @Override
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        String loc = resource.getLocation().toOSString()+"c"; //.py+c = .pyc
        if(loc.endsWith(".pyc")){
            //the .py has just been removed, so, remove the .pyc if it exists
            try {
                File file = new File(loc);
                IFile[] files = PydevPlugin.getWorkspaceFile(file);
                
                if(files == null){
                    return ;
                }

                //remove all: file and links
                for(final IFile workspaceFile : files){
                    if (workspaceFile != null && workspaceFile.exists()) {
                        
                        new Job("Deleting File"){
                            
                            @Override
                            protected IStatus run(IProgressMonitor monitor) {
                                monitor.beginTask("Delete .pyc file: "+workspaceFile.getName(), 1);
                                try {
                                    workspaceFile.delete(true, monitor);
                                } catch (CoreException e) {
                                    PydevPlugin.log(e);
                                }
                                monitor.done();
                                return Status.OK_STATUS;
                            }
                            
                        }.schedule();
                    }
                }
                
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
    }

}
