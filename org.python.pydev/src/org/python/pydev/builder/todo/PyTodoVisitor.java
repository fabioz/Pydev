/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.todo;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.internal.resources.Marker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.builder.PyDevBuilderVisitor;
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
    public boolean visitResource(IResource resource) {
        IProject project = resource.getProject();
        if (project != null && resource instanceof IFile) {
            List todoTags = PyTodoPrefPage.getTodoTags();
            
            IFile file = (IFile) resource;
            try {
                InputStream stream = file.getContents();
                int c; 
                StringBuffer buf = new StringBuffer();
                while((c = stream.read()) != -1){
                    buf.append((char)c);
                }
                Document document = new Document(buf.toString());
                int numberOfLines = document.getNumberOfLines();
                
                resource.deleteMarkers(IMarker.TASK, false, IResource.DEPTH_ZERO);
                
                int line = 1;
                while (line < numberOfLines) {
                    IRegion region = document.getLineInformation(line);
                    String tok = document.get(region.getOffset(), region.getLength());
                    int index;
                    
                    for (Iterator iter = todoTags.iterator(); iter.hasNext();) {
                        String element = (String) iter.next();

                        if ((index = tok.indexOf(element)) != -1) {
                            HashMap map = new HashMap();

                            createMarker(resource, tok.substring(index).trim(), line+1, Marker.TASK, IMarker.SEVERITY_WARNING, false, false);
                        }
                        
                    }
                    
                    line++;
                }
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }

        return true;
    }


}