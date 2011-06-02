/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.todo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.builder.PydevMarkerUtils;
import org.python.pydev.builder.PydevMarkerUtils.MarkerInfo;
import org.python.pydev.core.log.Log;
import org.python.pydev.logging.DebugSettings;

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
            List<String> todoTags = PyTodoPrefPage.getTodoTags();
            try {
				if(!isResourceInPythonpathProjectSources(resource, this.getPythonNature(resource), false)){
					return;
				}
			} catch (Exception e1) {
				Log.log(e1);
				return;
			}
			
            if(todoTags.size() > 0){

                int numberOfLines = document.getNumberOfLines();
    
                try {
                    //Timer timer = new Timer();
                    List<PydevMarkerUtils.MarkerInfo> lst = new ArrayList<PydevMarkerUtils.MarkerInfo>();
                    
                    int line = 0;
                    while (line < numberOfLines) {
                        IRegion region = document.getLineInformation(line);
                        String tok = document.get(region.getOffset(), region.getLength());
                        int index;
    
                        for (String element : todoTags) {
    
                            if ((index = tok.indexOf(element)) != -1) {
                                
                                String message=tok.substring(index).trim();
                                String markerType=IMarker.TASK;
                                int severity=IMarker.SEVERITY_WARNING;
                                boolean userEditable=false;
                                boolean isTransient=false;
                                int absoluteStart=region.getOffset()+index;
                                int absoluteEnd=absoluteStart+message.length();
                                Map<String, Object> additionalInfo = null;
                                
                                
                                MarkerInfo markerInfo = new PydevMarkerUtils.MarkerInfo(document, message, markerType, severity, userEditable, 
                                        isTransient, line, absoluteStart, absoluteEnd, additionalInfo);
                                lst.add(markerInfo);
                            }
                        }
                        line++;
                    }
                    if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                        Log.toLogFile(this, "Adding todo markers");
                    }
                    PydevMarkerUtils.replaceMarkers(lst, resource, IMarker.TASK, false, monitor);
                    //timer.printDiff("Total time to put markers: "+lst.size());
                } catch (Exception e) {
                    Log.log(e);
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