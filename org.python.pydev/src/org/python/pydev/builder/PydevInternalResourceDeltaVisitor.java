/*
 * Created on 11/09/2005
 */
package org.python.pydev.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.nature.PythonNature;

public abstract class PydevInternalResourceDeltaVisitor extends PyDevBuilderVisitor implements IResourceDeltaVisitor{
    
    PydevInternalResourceDeltaVisitor(IProgressMonitor monitor, int totalResources){
        this.monitor = monitor;
        this.totalResources = totalResources;
    }
    
    //variables used to communicate the progress
    /**
     * this monitor might be set externally so that we can comunicate the progress to the user
     * (set externally)
     */
    public IProgressMonitor monitor;
    /**
     * number of total resources to be visited (only used when the monitor is set)
     * (set externally)
     */
    public int totalResources;
    /**
     * number of resources visited to the moment 
     * (updated in this class)
     */
    public int currentResourcesVisited = 0;
    //end variables used to communicate the progress

    /**
     * Visits the resource delta tree determining which files to rebuild (*.py).
     * 
     * Subclasses should only reimplement visitChanged, visitAdded and visitRemoved. This method will not be called 
     * in the structure provided by pydev.
     * 
     * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
     */
    public boolean visit(IResourceDelta delta) throws CoreException {
        if(delta == null){
            return true;
        }

        IResource resource = delta.getResource();

        if(resource == null){
            return true;
        }
        
        int type = resource.getType();
    

        //related bug https://sourceforge.net/tracker/index.php?func=detail&aid=1238850&group_id=85796&atid=577329
        
        //the team-support plugins of eclipse use the IResource
        //method setTeamPrivateMember to indicate resources
        //that are only in the project for the team-stuff (e.g. .svn or
        //.cvs or _darcs directories).
        if (resource.isTeamPrivateMember()){
            return true;
        }
        
        if (type == IResource.FOLDER) {
            switch (delta.getKind()) {
                case IResourceDelta.REMOVED:
                    visitRemovedResource(resource, null, monitor);
                    break;
                //for folders, we don't have to do anything if added or changed (we just treat their children, that should
                //resolve for modules -- we do, however have to treat __init__.py differently).
            }
        }
        
        
        if (type == IResource.FILE) {
            String ext = resource.getFileExtension();
            if(ext == null){ //resource.getFileExtension() may return null if it has none.
                return true;
            }
            
            //only analyze projects with the python nature...
            IProject project = resource.getProject();
            PythonNature nature = PythonNature.getPythonNature(project);
            
            if(project != null && nature != null){
                //we just want to make the visit if it is a valid python file and it is in the pythonpath
                if (PythonPathHelper.isValidSourceFile("."+ext)) {
                    
                    boolean isAddOrChange = false;
                    switch (delta.getKind()) {
                        case IResourceDelta.ADDED :
                            visitAddedResource(resource, PyDevBuilder.getDocFromResource(resource), monitor);
                            isAddOrChange = true;
                            break;
                        case IResourceDelta.CHANGED:
                            visitChangedResource(resource, PyDevBuilder.getDocFromResource(resource), monitor);
                            isAddOrChange = true;
                            break;
                        case IResourceDelta.REMOVED:
                            visitRemovedResource(resource, null, monitor);
                            break;
                    }

                    if(isAddOrChange){
                        //communicate the progress
                        currentResourcesVisited++;
                        PyDevBuilder.communicateProgress(monitor, totalResources, currentResourcesVisited, resource, this);
                    }
                }
            }
        }
        
        return true;
    }

}
