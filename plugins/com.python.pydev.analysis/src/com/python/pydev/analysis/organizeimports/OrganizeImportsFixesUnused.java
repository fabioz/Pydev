/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 25/09/2005
 */
package com.python.pydev.analysis.organizeimports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.parser.IParserObserver;
import org.python.pydev.core.parser.ISimpleNode;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.IOrganizeImports;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;

import com.aptana.shared_core.structure.Tuple;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisParserObserver;
import com.python.pydev.analysis.builder.AnalysisRunner;

/**
 *
 * @author Fabio
 */
public class OrganizeImportsFixesUnused implements IOrganizeImports {

    public boolean beforePerformArrangeImports(PySelection ps, PyEdit edit) {

        IDocumentExtension4 doc = (IDocumentExtension4) edit.getDocument();
        long docTime = doc.getModificationStamp();
        ensureParsed(edit, docTime);
        if (docTime != edit.getAstModificationTimeStamp()) {
            return true;
        }
        SimpleNode ast = edit.getAST();
        if (ast == null) {
            return true; //we need it to be correctly parsed with an ast to be able to do it...
        }

        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        try {
            ast.accept(visitor);
        } catch (Exception e1) {
            Log.log(e1);
            return true; //just go on
        }
        List<ASTEntry> availableImports = visitor.getAsList(new Class[] { ImportFrom.class, Import.class });

        PySourceViewer s = edit.getPySourceViewer();

        ArrayList<MarkerAnnotationAndPosition> unusedImportsMarkers = new ArrayList<MarkerAnnotationAndPosition>();
      
        //get the markers we are interested in and the related ast elements
        for (Iterator<MarkerAnnotationAndPosition> it = s.getMarkerIterator(); it.hasNext();) {
            MarkerAnnotationAndPosition marker = it.next();
            try {
                String type = marker.markerAnnotation.getMarker().getType();
                if (type != null && type.equals(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER)) {
                    Integer attribute = marker.markerAnnotation.getMarker().getAttribute(
                            AnalysisRunner.PYDEV_ANALYSIS_TYPE, -1);
                    if (attribute != null) {
                        if (attribute.equals(IAnalysisPreferences.TYPE_UNUSED_IMPORT)) {
                            unusedImportsMarkers.add(marker);
                        } else if (attribute.equals(IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT)) {
                            unusedImportsMarkers.add(marker);
                        }
                    }

                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        try {
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
            deleteImports(doc, ps, unusedImportsMarkers,edit);
        } catch (BadLocationException e) {
            Log.log(e);
        } catch (CoreException e) {
            Log.log(e);
        }
        return true;

    }

    
    private void deleteImports(IDocumentExtension4 doc, PySelection ps, Iterable<MarkerAnnotationAndPosition> markers,
            PyEdit edit) throws BadLocationException, CoreException {
        DocumentRewriteSession session = doc.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
        try {
            for(MarkerAnnotationAndPosition m:markers ) {
                deleteImport(ps,m,edit);
            }
        }
        finally {
            doc.stopRewriteSession(session);
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
                    new Tuple<String, Boolean>(AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE, true)
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
    private void deleteImport(PySelection ps, MarkerAnnotationAndPosition markerInfo, PyEdit edit)
            throws BadLocationException, CoreException {
        SimpleNode ast = edit.getAST();
        if (ast == null) {
            //we need the ast to look for the imports... (the generated markers will be matched against them)
            return;
        }
        IMarker marker = markerInfo.markerAnnotation.getMarker();

        Integer start = (Integer) marker.getAttribute(IMarker.CHAR_START);
        Integer end = (Integer) marker.getAttribute(IMarker.CHAR_END);
        ps.setSelection(start, end);
        ps.deleteSelection();
    }

    public void afterPerformArrangeImports(PySelection ps, PyEdit pyEdit) {
        //do nothing
    }

}
