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
import org.python.pydev.plugin.nature.PythonNature;

public class PycRemoverBuilderVisitor extends PyDevBuilderVisitor{

    @Override
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        String loc = resource.getLocation().toOSString();
        if(loc != null && loc.length() > 3){
            String dotPy = loc.substring(0, loc.length()-1);
            File file = new File(dotPy); //.py file
            if(file.exists()){
                return;
            }
            file = new File(dotPy+"w"); //.pyw file
            if(file.exists()){
                return;
            }
            
            //this is needed because this method might be called alone (not in the grouper that checks
            //if it is in the pythonpath before)
            //
            //this happens only when a .pyc file is found... if it was a .py file, this would not be needed (as is the
            //case in the visit removed resource)
            
        	if(!PythonNature.isResourceInPythonpath(dotPy, resource.getProject())){
            	return; // we only analyze resources that are in the pythonpath
            }

            //if still did not return, let's remove it
            treatPycFile(loc);
        }
    }

    @Override
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        String loc = resource.getLocation().toOSString()+"c"; //.py+c = .pyc
        treatPycFile(loc);
    }

    /**
     * @param loc
     */
    private void treatPycFile(String loc) {
        if(loc.endsWith(".pyc")){
            //the .py has just been removed, so, remove the .pyc if it exists
            try {
                File file = new File(loc);
                IFile[] files = PydevPlugin.getWorkspaceFiles(file);
                
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
