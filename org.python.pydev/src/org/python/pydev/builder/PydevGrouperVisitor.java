/*
 * Created on 11/09/2005
 */
package org.python.pydev.builder;

import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;

public class PydevGrouperVisitor extends PydevInternalResourceDeltaVisitor {

    private List<PyDevBuilderVisitor> visitors;

    public PydevGrouperVisitor(List<PyDevBuilderVisitor> visitors, IProgressMonitor monitor, int totalResources) {
        super(monitor, totalResources);
        this.visitors = visitors;
    }
    
    private void visitWith(String name, boolean isAddOrChange, IResource resource, IDocument document){
        HashMap<String, Object> memo = new HashMap<String, Object>();
        
        for (PyDevBuilderVisitor visitor : visitors) {
            // some visitors cannot visit too many elements because they do a lot of processing
            if (visitor.maxResourcesToVisit() == PyDevBuilderVisitor.MAX_TO_VISIT_INFINITE || visitor.maxResourcesToVisit() >= totalResources) {
                visitor.memo = memo; //setting the memo must be the first thing.
                try {
                    REF.invoke(visitor, name, resource, document);
                    
                    //ok, standard visiting ended... now, we have to check if we should visit the other
                    //resources if it was an __init__.py file that changed
                    if(isAddOrChange && visitor.shouldVisitInitDependency() && isInitFile(resource)){
                        IResource[] initDependents = getInitDependents(resource);
                        
                        for (int i = 0; i < initDependents.length; i++) {
                            REF.invoke(visitor, name, initDependents[i], PyDevBuilder.getDocFromResource(initDependents[i]));
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }
        
    }

    @Override
    public boolean visitAddedResource(IResource resource, IDocument document) {
        visitWith("visitAddedResource", true, resource, document);
        return true;
    }
    
    @Override
    public boolean visitChangedResource(IResource resource, IDocument document) {
        visitWith("visitChangedResource", true, resource, document);
        return true;
    }

    @Override
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        visitWith("visitRemovedResource", false, resource, document);
        return true;
    }

}
