/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.core.IMiscConstants;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.PyParser.IPostParserListener;
import org.python.pydev.shared_core.model.ErrorDescription;
import org.python.pydev.shared_core.structure.Tuple;

/**
 *
 * @author Fabio, Jeremy J. Carroll
 */
class OrganizeImportsFixesUnused {

    public boolean beforePerformArrangeImports(PySelection ps, PyEdit edit, IFile f) {
        int oldSelection = ps.getRegion().getOffset();

        IDocumentExtension4 doc = (IDocumentExtension4) ps.getDoc();
        if (edit != null) {
            if (!ensureParsed(edit)) {
                return true;
            }
            //Check that the editor time is actually the same as the document time.
            long docTime = doc.getModificationStamp();

            if (docTime != edit.getAstModificationTimeStamp()) {
                return true;
            }
            ErrorDescription errorDescription = edit.getErrorDescription();
            if (errorDescription != null) {
                return true; //Don't remove unused imports if we have syntax errors.
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

    private void findAndDeleteUnusedImports(PySelection ps, PyEdit edit, IDocumentExtension4 doc, IFile f)
            throws Exception {
        Iterator<MarkerAnnotationAndPosition> it;
        if (edit != null) {
            it = edit.getPySourceViewer().getMarkerIterator();
        } else {

            IMarker markers[] = f.findMarkers(IMiscConstants.PYDEV_ANALYSIS_PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
            MarkerAnnotationAndPosition maap[] = new MarkerAnnotationAndPosition[markers.length];
            int ix = 0;
            for (IMarker m : markers) {
                int start = (Integer) m.getAttribute(IMarker.CHAR_START);
                int end = (Integer) m.getAttribute(IMarker.CHAR_END);
                maap[ix++] =
                        new MarkerAnnotationAndPosition(new MarkerAnnotation(m), new Position(start, end - start));
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
        while (it.hasNext()) {
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
        Collections.sort(unusedImportsMarkers, new Comparator<MarkerAnnotationAndPosition>() {

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
            }
        });
    }

    private void deleteImports(IDocumentExtension4 doc, PySelection ps, Iterable<MarkerAnnotationAndPosition> markers)
            throws BadLocationException, CoreException {
        for (MarkerAnnotationAndPosition m : markers) {
            deleteImport(ps, m);
        }
    }

    private boolean ensureParsed(PyEdit edit) {
        //Ok, we have a little problem here: we have to ensure not that only a regular ast parse took place, but
        //that the analysis of the related document is updated (so that the markers are in-place).
        //To do that, we ask for a reparse asking to analyze the results in that same thread (without scheduling it).
        //
        //Maybe better would be having some extension that allowed to call the analysis of the document directly
        //(so we don't have to rely on markers in place?)
        final PyParser parser = edit.getParser();
        final boolean[] notified = new boolean[] { false };
        final Object sentinel = new Object();

        parser.addPostParseListener(new IPostParserListener() {

            @Override
            public void participantsNotified(Object... argsToReparse) {
                synchronized (OrganizeImportsFixesUnused.this) {
                    parser.removePostParseListener(this);
                    if (argsToReparse.length == 2 && argsToReparse[1] == sentinel) {
                        notified[0] = true;
                        OrganizeImportsFixesUnused.this.notify();
                    }
                }
            }
        });

        long initial = System.currentTimeMillis();
        while ((System.currentTimeMillis() - initial) < 5000) {

            if (parser.forceReparse(new Tuple<String, Boolean>(
                    IMiscConstants.ANALYSIS_PARSER_OBSERVER_FORCE_IN_THIS_THREAD, true), sentinel)) {
                //ok, we were able to schedule it with our parameters, let's wait for its completion...
                synchronized (this) {
                    try {
                        wait(5000);
                    } catch (InterruptedException e) {
                    }
                }
                break;
            } else {
                synchronized (this) {
                    try {
                        wait(200);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        //Commented out: in the worse case, we already waited 5 seconds, if because of a racing condition we couldn't decide 
        //that it worked, let's just keep on going hoping that the markers are in place...
        //if (!notified[0]) {
        //    return false;
        //}
        return true;
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
        IDocument doc = ps.getDoc();
        while (start > 0) {
            char c;
            try {
                c = doc.getChar(start - 1);
            } catch (Exception e) {
                break;
            }
            if (c == '\r' || c == '\n') {
                break;
            }
            if (Character.isWhitespace(c) || c == ',') {
                start--;
                continue;
            }
            break;
        }
        ps.setSelection(start, end);
        ps.deleteSelection();
    }

}
