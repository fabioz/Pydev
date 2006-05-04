/*
 * Created on 11/09/2005
 */
package org.python.pydev.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Groups the visitors to be added and visits them according to their priority
 * 
 * @author fabioz
 */
public class PydevGrouperVisitor extends PydevInternalResourceDeltaVisitor {

    private List<PyDevBuilderVisitor> visitors;

    public PydevGrouperVisitor(List<PyDevBuilderVisitor> _visitors, IProgressMonitor monitor, int totalResources) {
        super(monitor, totalResources);
        //make a copy - should be already sorted at this point
        this.visitors = new ArrayList<PyDevBuilderVisitor>(_visitors);
    }
    
    /**
     * @param name determines the name of the method to visit (added removed or changed)
     * @param isAddOrChange true if it is an add or change
     * @param resource the resource to visit
     * @param document the document from the resource
     * @param monitor 
     */
    private void visitWith(String name, boolean isAddOrChange, IResource resource, IDocument document, IProgressMonitor monitor){
    	if(monitor.isCanceled()){
    		return; //it's already cancelled
    	}
        if(!PythonNature.isResourceInPythonpath(resource)){
        	return; // we only analyze resources that are in the pythonpath
        }
        HashMap<String, Object> memo = new HashMap<String, Object>();
        memo.put(PyDevBuilderVisitor.IS_FULL_BUILD, false); //mark it as a delta build
        
        for (PyDevBuilderVisitor visitor : visitors) {
            // some visitors cannot visit too many elements because they do a lot of processing
            if (visitor.maxResourcesToVisit() == PyDevBuilderVisitor.MAX_TO_VISIT_INFINITE || visitor.maxResourcesToVisit() >= totalResources) {
                visitor.memo = memo; //setting the memo must be the first thing.
                try {
                    //communicate progress for each visitor
                    PyDevBuilder.communicateProgress(monitor, totalResources, currentResourcesVisited, resource, visitor);
                    REF.invoke(visitor, name, resource, document, monitor);
                    
                    //ok, standard visiting ended... now, we have to check if we should visit the other
                    //resources if it was an __init__.py file that changed
                    if(isAddOrChange && visitor.shouldVisitInitDependency() && isInitFile(resource)){
                        IResource[] initDependents = getInitDependents(resource);
                        
                        for (int i = 0; i < initDependents.length; i++) {
                            REF.invoke(visitor, name, initDependents[i], REF.getDocFromResource(initDependents[i]), monitor);
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }
        
    }

    @Override
    public void visitAddedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        visitWith("visitAddedResource", true, resource, document, monitor);
    }
    
    @Override
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        visitWith("visitChangedResource", true, resource, document, monitor);
    }

    @Override
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        visitWith("visitRemovedResource", false, resource, document, monitor);
    }

}
