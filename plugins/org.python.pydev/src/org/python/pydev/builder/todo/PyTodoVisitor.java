/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_ui.utils.PyMarkerUtils;
import org.python.pydev.shared_ui.utils.PyMarkerUtils.MarkerInfo;

/**
 * @author Fabio Zadrozny
 */
public class PyTodoVisitor extends PyDevBuilderVisitor {

    /*
     * (non-Javadoc)
     *
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitResource(org.eclipse.core.resources.IResource)
     */
    @Override
    public void visitChangedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        if (document != null) {
            List<String> todoTags = PyTodoPrefPage.getTodoTags();
            try {
                if (!isResourceInPythonpathProjectSources(resource, this.getPythonNature(resource), false)) {
                    PyMarkerUtils.removeMarkers(resource, IMarker.TASK);
                    return;
                }
            } catch (Exception e1) {
                Log.log(e1);
                return;
            }

            try {
                PyMarkerUtils.replaceMarkers(computeTodoMarkers(document.call(), todoTags), resource, IMarker.TASK,
                        false, monitor);
                //timer.printDiff("Total time to put markers: "+lst.size());
            } catch (Exception e) {
                Log.log(e);
            }
        }

    }

    /**
     * Computes the TODO markers available for this document.
     * Considers only TODO flags in strings and comments.
     */
    /*default*/List<MarkerInfo> computeTodoMarkers(IDocument document, List<String> todoTags)
            throws BadLocationException {
        List<PyMarkerUtils.MarkerInfo> lst = new ArrayList<PyMarkerUtils.MarkerInfo>();
        if (todoTags.size() > 0 && document != null) {

            String str = document.get();
            ParsingUtils utils = ParsingUtils.create(str);
            int len = utils.len();
            try {
                for (int i = 0; i < len; i++) {
                    char c = str.charAt(i);
                    switch (c) {
                        case '\'':
                        case '\"':
                            int j = utils.eatLiterals(null, i);
                            check(i, j, document, lst, todoTags);
                            i = j;
                            break;

                        case '#':
                            j = utils.eatComments(null, i);
                            check(i, j, document, lst, todoTags);
                            i = j;
                            break;
                    }
                }
            } catch (BadLocationException e) {
                //ignore (if document changed in the iteration).
            } catch (SyntaxErrorException e) {
                Log.log(e); //Should not happen!
            }

            if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                Log.toLogFile(this, "Adding todo markers");
            }
        }
        return lst;
    }

    /**
     * Checks a partition of a document for todo tags (filling lst with the markers to be created).
     */
    private void check(int i, int j, IDocument document, List<MarkerInfo> lst, List<String> todoTags)
            throws BadLocationException {
        String tok = document.get(i, j - i);
        int index;
        HashSet<Integer> lines = new HashSet<Integer>();
        for (String element : todoTags) {

            if (element.length() == 0) {
                continue;
            }
            int start = 0;
            while ((index = tok.indexOf(element, start)) != -1) {

                start = index + element.length();

                int absoluteStart = i + index;
                int line = document.getLineOfOffset(absoluteStart);
                if (lines.contains(line)) {
                    //Only 1 TASK per line!
                    continue;
                } else {
                    lines.add(line);
                }

                String message = tok.substring(index).trim();
                String markerType = IMarker.TASK;
                int severity = IMarker.SEVERITY_WARNING;
                boolean userEditable = false;
                boolean isTransient = false;
                int absoluteEnd = absoluteStart + message.length();
                Map<String, Object> additionalInfo = null;

                MarkerInfo markerInfo = new PyMarkerUtils.MarkerInfo(document, message, markerType, severity,
                        userEditable, isTransient, line, absoluteStart, absoluteEnd, additionalInfo);
                lst.add(markerInfo);
            }
        }
    }

    /**
     * @see org.python.pydev.builder.PyDevBuilderVisitor#visitRemovedResource(org.eclipse.core.resources.IResource, org.eclipse.jface.text.IDocument)
     */
    @Override
    public void visitRemovedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
    }

}