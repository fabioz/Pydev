/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.todo;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.builder.PydevMarkerUtils;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public class PyTodoVisitor extends PyDevBuilderVisitor {

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitResource(org.eclipse.core.resources.IResource)
     */
    public void visitChangedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
        if (document != null) {
            List todoTags = PyTodoPrefPage.getTodoTags();
            if(todoTags.size() > 0){

	            int numberOfLines = document.getNumberOfLines();
	
	            try {
	                resource.deleteMarkers(IMarker.TASK, false, IResource.DEPTH_ZERO);
	
	                int line = 0;
	                while (line < numberOfLines) {
	                    IRegion region = document.getLineInformation(line);
	                    String tok = document.get(region.getOffset(), region.getLength());
	                    int index;
	
	                    for (Iterator iter = todoTags.iterator(); iter.hasNext();) {
	                        String element = (String) iter.next();
	
	                        if ((index = tok.indexOf(element)) != -1) {
	                            PydevMarkerUtils.createMarker(resource, document, tok.substring(index).trim(), line, IMarker.TASK, IMarker.SEVERITY_WARNING, false, false, null);
	                        }
	
	                    }
	
	                    line++;
	                }
	            } catch (Exception e) {
	                PydevPlugin.log(e);
	            } 
            }
        }

    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public void visitRemovedResource(IResource resource, IDocument document, IProgressMonitor monitor) {
    }

}