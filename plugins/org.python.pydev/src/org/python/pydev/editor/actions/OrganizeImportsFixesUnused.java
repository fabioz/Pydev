/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Copyright (c) 2013 by Syapse, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 25/09/2005
 */
package org.python.pydev.editor.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.core.IMiscConstants;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.parser.IParserObserver;
import org.python.pydev.core.parser.ISimpleNode;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.parser.PyParser;

import com.aptana.shared_core.structure.Tuple;

/**
 *
 * @author Fabio, Jeremy J. Carroll
 */
class OrganizeImportsFixesUnused {

    public boolean beforePerformArrangeImports(PySelection ps, PyEdit edit, IFile f) {
        int oldSelection = ps.getRegion().getOffset();

        IDocumentExtension4 doc = (IDocumentExtension4) ps.getDoc();
        if (edit != null) {
            long docTime = doc.getModificationStamp();
            ensureParsed(edit, docTime);
            if (docTime != edit.getAstModificationTimeStamp()) {
                return true;
            }
        }

        try {
            findAndDeleteUnusedImports(ps, edit, doc, f);
        } catch (Exception e) {
            Log.log(e);
        }
        ps.setSelection(oldSelection, oldSelection);
        return true;

    }


    private void findAndDeleteUnusedImports(PySelection ps, PyEdit edit, IDocumentExtension4 doc, IFile f) throws Exception {
        Iterator<MarkerAnnotationAndPosition> it;
        if (edit != null) {
            it = edit.getPySourceViewer().getMarkerIterator();
        } else {

            IMarker markers[] = f.findMarkers(IMiscConstants.PYDEV_ANALYSIS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
            MarkerAnnotationAndPosition maap[] = new  MarkerAnnotationAndPosition[markers.length];
            int ix = 0;
            for (IMarker m:markers) {
                int start = (Integer)m.getAttribute(IMarker.CHAR_START);
                int end = (Integer) m.getAttribute(IMarker.CHAR_END);
                maap[ix++] = 
                        new MarkerAnnotationAndPosition(new MarkerAnnotation(m), new Position(start,end-start));
            }
            it = Arrays.asList(maap).iterator();
        }
        ArrayList<MarkerAnnotationAndPosition> unusedImportsMarkers = getUnusedImports(it);
        sortInReverseDocumentOrder(unusedImportsMarkers);
        deleteImports(doc, ps, unusedImportsMarkers);

    }


    private ArrayList<MarkerAnnotationAndPosition> getUnusedImports(Iterator<MarkerAnnotationAndPosition> it)
            throws CoreException {
        ArrayList<MarkerAnnotationAndPosition> unusedImportsMarkers = new ArrayList<MarkerAnnotationAndPosition>();
        while(it.hasNext()) {
            MarkerAnnotationAndPosition marker = it.next();
            String type = marker.markerAnnotation.getMarker().getType();
            if (type != null && type.equals(IMiscConstants.PYDEV_ANALYSIS_PROBLEM_MARKER)) {
                Integer attribute = marker.markerAnnotation.getMarker().getAttribute(
                        IMiscConstants.PYDEV_ANALYSIS_TYPE, -1);
                if (attribute != null) {
                    if (attribute.equals(IMiscConstants.TYPE_UNUSED_IMPORT)) {
                        unusedImportsMarkers.add(marker);
                    }
                }

            }
        }
        return unusedImportsMarkers;
    }


    private void sortInReverseDocumentOrder(ArrayList<MarkerAnnotationAndPosition> unusedImportsMarkers) {
        Collections.sort(unusedImportsMarkers,new Comparator<MarkerAnnotationAndPosition>(){

            public int compare(MarkerAnnotationAndPosition arg0, MarkerAnnotationAndPosition arg1) {
                try {
                    return getCharStart(arg1) - getCharStart(arg0);
                } catch (CoreException e) {
                    Log.log(e);
                    return 0;
                }
            }

            private int getCharStart(MarkerAnnotationAndPosition arg0) throws CoreException {
                IMarker marker = arg0.markerAnnotation.getMarker();
                int i = (Integer) marker.getAttribute(IMarker.CHAR_START);
                return i;
            }});
    }

    
    private void deleteImports(IDocumentExtension4 doc, PySelection ps, Iterable<MarkerAnnotationAndPosition> markers) throws BadLocationException, CoreException {
        for(MarkerAnnotationAndPosition m:markers ) {
            deleteImport(ps,m);
        }
    }

    private void ensureParsed(PyEdit edit, long docTime) {
        long parseTime = edit.getAstModificationTimeStamp();
        
        if (docTime != parseTime) {
            PyParser parser = edit.getParser();
            parser.addParseListener(new IParserObserver(){

                public void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc) {
                    synchronized (OrganizeImportsFixesUnused.this) {
                        OrganizeImportsFixesUnused.this.notify();
                    }
                }

                public void parserError(Throwable error, IAdaptable file, IDocument doc) {
                }});
            parser.forceReparse(
                    new Tuple<String, Boolean>(IMiscConstants.ANALYSIS_PARSER_OBSERVER_FORCE, true)
                    );
            synchronized ( this ) {
                try {
                    wait(5000);
                } catch (InterruptedException e) {
                }
            }
        }
    }



    /**
     * 
     * This leaves a bit of a mess e.g. "import " or "from " for some later process to clean up.
     * 
     */
    private void deleteImport(PySelection ps, MarkerAnnotationAndPosition markerInfo)
            throws BadLocationException, CoreException {
        IMarker marker = markerInfo.markerAnnotation.getMarker();

        Integer start = (Integer) marker.getAttribute(IMarker.CHAR_START);
        Integer end = (Integer) marker.getAttribute(IMarker.CHAR_END);
        ps.setSelection(start, end);
        ps.deleteSelection();
    }


}
