/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 18/09/2005
 */
package com.python.pydev.analysis.additionalinfo.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.analysis.messages.IMessage;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.IMiscConstants;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;

public class AnalysisRunner {

    public static final String PYDEV_CODE_ANALYSIS_IGNORE = "@PydevCodeAnalysisIgnore";
    public static final String PYDEV_CODE_ANALYSIS_IGNORE2 = "flake8: noqa"; // Also ignore if there's a flake8: noqa.

    /**
     * Indicates the type of the message given the constants in org.python.pydev.ast.analysis.IAnalysisPreferences (unused import,
     * undefined variable...)
     */
    public static final String PYDEV_ANALYSIS_TYPE = IMiscConstants.PYDEV_ANALYSIS_TYPE;

    /**
     * Indicates the additional info for the marker (depends on its type) - may be null
     */
    public static final String PYDEV_ANALYSIS_ADDITIONAL_INFO = "PYDEV_INFO";

    /**
     * this is the type of the marker
     */
    public static final String PYDEV_ANALYSIS_PROBLEM_MARKER = IMiscConstants.PYDEV_ANALYSIS_PROBLEM_MARKER;

    public static final String PYLINT_PROBLEM_MARKER = IMiscConstants.PYLINT_PROBLEM_MARKER;
    public static final String MYPY_PROBLEM_MARKER = IMiscConstants.MYPY_PROBLEM_MARKER;

    /**
     * do we want to debug this class?
     */
    private static final boolean DEBUG_ANALYSIS_RUNNER = false;

    /**
     * @param document the document we want to check
     * @return true if we can analyze it and false if there is some flag saying that we shouldn't
     */
    public boolean canDoAnalysis(IDocument document) {
        if (document == null) {
            return false;
        }
        for (int i = 0; i < 3; i++) { //Only check first 3 lines.
            String line = PySelection.getLine(document, i);
            int commentIndex;
            if ((commentIndex = line.indexOf('#')) != -1) {
                String commentPart = line.substring(commentIndex);
                if (commentPart
                        .contains(PYDEV_CODE_ANALYSIS_IGNORE) || commentPart.contains(PYDEV_CODE_ANALYSIS_IGNORE2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void deleteMarkers(IResource resource) {
        deleteMarkers(resource, false);
    }

    /**
     * @param resource the resource that should have the markers deleted
     */
    public static void deleteMarkers(IResource resource, boolean onlyPydevAnalysisMarkers) {
        if (resource == null) {
            return;
        }

        try {
            resource.deleteMarkers(PYDEV_ANALYSIS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
        } catch (CoreException e) {
            //ok, if it is a resource exception, it may have happened because the resource does not exist anymore
            //so, there is no need to log this failure
            if (resource.exists()) {
                Log.log(e);
            }
        } catch (Exception e) {
            Log.log(e);
        }
        if (onlyPydevAnalysisMarkers) {
            return;
        }

        // Also delete PyLint markers (as they're computed along problem markers now).
        try {
            if (resource != null) {
                resource.deleteMarkers(PYLINT_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
            }
        } catch (CoreException e3) {
            Log.log(e3);
        }

        // Also delete Mypy markers (as they're computed along problem markers now).
        try {
            if (resource != null) {
                resource.deleteMarkers(MYPY_PROBLEM_MARKER, false, IResource.DEPTH_ZERO);
            }
        } catch (CoreException e3) {
            Log.log(e3);
        }

    }

    /**
     * Sets the analysis markers in the resource (removes current markers and adds the new ones)
     *
     * @param resource the resource where we want to add the markers
     * @param document the document
     * @param messages the messages to add
     * @param monitor monitor to check if we should stop the process.
     * @param existing these are the existing markers. After this method, the list will contain only the ones that
     * should be removed.
     * @return
     */
    public List<MarkerInfo> setMarkers(IResource resource, IDocument document, IMessage[] messages,
            IProgressMonitor monitor) {
        if (resource == null) {
            return null;
        }
        try {
            //Timer timer = new Timer();
            //System.out.println("Start creating markers");
            ArrayList<MarkerInfo> lst = generateMarkers(document, messages, monitor);

            if (monitor.isCanceled()) {
                return null;
            }

            PyMarkerUtils.replaceMarkers(lst, resource, AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, true, monitor);
            //timer.printDiff("Time to put markers: "+lst.size());
            return lst;
        } catch (Exception e) {
            Log.log("Error when setting markers on: " + resource, e);
        }
        return null;
    }

    public ArrayList<MarkerInfo> generateMarkers(IDocument document, IMessage[] messages, IProgressMonitor monitor) {
        ArrayList<MarkerInfo> lst = new ArrayList<MarkerInfo>();
        //add the markers... the id is put as additional info for it
        for (IMessage m : messages) {

            HashMap<String, Object> additionalInfo = new HashMap<String, Object>();
            additionalInfo.put(PYDEV_ANALYSIS_TYPE, m.getType());

            //not all messages have additional info
            List<String> infoForType = m.getAdditionalInfo();
            if (infoForType != null) {
                additionalInfo.put(PYDEV_ANALYSIS_ADDITIONAL_INFO, infoForType);
            }

            int startLine = m.getStartLine(document) - 1;
            int startCol = m.getStartCol(document) - 1;
            int endLine = m.getEndLine(document) - 1;
            int endCol = m.getEndCol(document) - 1;

            String msg = m.getMessage();
            if (DEBUG_ANALYSIS_RUNNER) {
                System.out.printf("\nAdding at start:%s end:%s line:%s message:%s ", startCol, endCol, startLine,
                        msg);
            }

            if (monitor.isCanceled()) {
                return null;
            }

            MarkerInfo markerInfo = new PyMarkerUtils.MarkerInfo(document, msg,
                    AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER, m.getSeverity(), false, false, startLine,
                    startCol, endLine, endCol, additionalInfo);
            lst.add(markerInfo);
        }
        return lst;
    }

}
