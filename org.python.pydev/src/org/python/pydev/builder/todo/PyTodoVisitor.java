/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.todo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.internal.resources.Marker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
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
    public boolean visitResource(IResource resource, IDocument document) {
        if (document != null) {
            List todoTags = PyTodoPrefPage.getTodoTags();

            int numberOfLines = document.getNumberOfLines();

            try {
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

                            createMarker(resource, tok.substring(index).trim(), line + 1, Marker.TASK, IMarker.SEVERITY_WARNING, false, false);
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

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    public boolean visitRemovedResource(IResource resource, IDocument document) {
        return false;
    }

}