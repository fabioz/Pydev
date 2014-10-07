/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyShiftLeft;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.shared_ui.editor.BaseSourceViewer;
import org.python.pydev.shared_ui.editor.ITextViewerExtensionAutoEditions;
import org.python.pydev.shared_ui.proposals.ICompletionStyleToggleEnabler;

public class PySourceViewer extends BaseSourceViewer implements IAdaptable, ICompletionStyleToggleEnabler,
        ITextViewerExtensionAutoEditions {

    private WeakReference<PyEdit> projection;

    public PySourceViewer(Composite parent, IVerticalRuler ruler, IOverviewRuler overviewRuler,
            boolean showsAnnotationOverview, int styles, final PyEditProjection projection) {
        super(parent, ruler, overviewRuler, showsAnnotationOverview, styles,
                new PyAbstractIndentGuidePreferencesProvider() {

                    @Override
                    public int getTabWidth() {
                        return ((PyEdit) projection).getIndentPrefs().getTabWidth();
                    }
                });
        this.projection = new WeakReference<PyEdit>((PyEdit) projection);
    }

    private boolean isInToggleCompletionStyle;

    public void setInToggleCompletionStyle(boolean b) {
        this.isInToggleCompletionStyle = b;
    }

    public boolean getIsInToggleCompletionStyle() {
        return this.isInToggleCompletionStyle;
    }

    public PyEdit getEdit() {
        return projection.get();
    }

    /**
     * @param markerLine the line we want markers on
     * @param markerType the type of the marker (if null, it is not used)
     * @return a list of markers at the given line
     */
    public List<MarkerAnnotationAndPosition> getMarkersAtLine(int markerLine, String markerType) {
        ArrayList<MarkerAnnotationAndPosition> markers = new ArrayList<MarkerAnnotationAndPosition>();
        IDocumentProvider documentProvider = this.getEdit().getDocumentProvider();
        if (documentProvider == null) {
            return markers;
        }
        IEditorInput editorInput = this.getEdit().getEditorInput();
        if (editorInput == null) {
            return markers;
        }
        IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editorInput);
        if (annotationModel == null) {
            return markers;
        }
        IDocument document = documentProvider.getDocument(editorInput);
        if (document == null) {
            return markers;
        }

        int lineStartOffset = -1;
        int lineEndOffset = -1;
        try {
            lineStartOffset = document.getLineOffset(markerLine);
            lineEndOffset = lineStartOffset + document.getLineLength(markerLine);
        } catch (BadLocationException e) {
            return markers;
        }

        for (Iterator<MarkerAnnotationAndPosition> it = getMarkerIterator(); it.hasNext();) {
            MarkerAnnotationAndPosition annotation = it.next();

            Position position = annotation.position;
            if (position == null) {
                continue;
            }
            int offset = position.getOffset();
            if (offset >= lineStartOffset && offset <= lineEndOffset) {
                IMarker marker = annotation.markerAnnotation.getMarker();
                String type;
                try {
                    type = marker.getType();
                } catch (CoreException e) {
                    continue;
                }
                if (markerType == null || markerType.equals(type)) {
                    markers.add(annotation);
                }
            }

        }

        return markers;
    }

    /**
     * @return a class that iterates through the markers available in this source viewer
     */
    public Iterator<MarkerAnnotationAndPosition> getMarkerIterator() {
        final IAnnotationModel annotationModel = getAnnotationModel();
        //it may be null on external files, because I simply cannot make it get the org.python.copiedfromeclipsesrc.PydevFileEditorInput
        //(if it did, I could enhance it...). Instead, it returns a org.eclipse.ui.internal.editors.text.JavaFileEditorInput
        //that never has an annotation model. (shortly, eclipse bug).
        if (annotationModel != null) {
            final Iterator annotationIterator = annotationModel.getAnnotationIterator();

            return new Iterator<MarkerAnnotationAndPosition>() {

                private MarkerAnnotationAndPosition marker;

                public boolean hasNext() {
                    while (annotationIterator.hasNext()) {
                        if (marker != null) {
                            return true;
                        }

                        while (annotationIterator.hasNext()) {
                            Object object = annotationIterator.next();
                            if (object instanceof MarkerAnnotation) {
                                MarkerAnnotation m = (MarkerAnnotation) object;
                                if (m.isMarkedDeleted()) {
                                    continue;
                                }
                                marker = new MarkerAnnotationAndPosition(m, annotationModel.getPosition(m));
                                return true;
                            }
                        }
                    }
                    return false;
                }

                public MarkerAnnotationAndPosition next() {
                    hasNext();

                    MarkerAnnotationAndPosition m = marker;
                    marker = null;
                    return m;
                }

                public void remove() {
                    throw new RuntimeException("not implemented");
                }

            };
        }
        return new Iterator<MarkerAnnotationAndPosition>() {
            public boolean hasNext() {
                return false;
            }

            public MarkerAnnotationAndPosition next() {
                return null;
            }

            public void remove() {
                throw new RuntimeException("not implemented");
            }
        };
    }

    /**
     * Overridden to provide a shift left that can work even if the number of chars for the dedent
     * is lower than the number of chars of the indentation string.
     */
    @Override
    public void doOperation(int operation) {
        if (operation == SHIFT_LEFT) {
            doShiftLeft();
            return;
        }
        super.doOperation(operation);
    }

    /**
     * Do a shift while properly handling undo/redo and rewrite sessions.
     * Uses the PyShiftLeft action to actually do the shift.
     */
    private void doShiftLeft() {
        if (fUndoManager != null) {
            fUndoManager.beginCompoundChange();
        }

        IDocument d = getDocument();
        DocumentRewriteSession rewriteSession = null;
        try {
            if (d instanceof IDocumentExtension4) {
                IDocumentExtension4 extension = (IDocumentExtension4) d;
                rewriteSession = extension.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
            }
            // Perform the shift operation.
            PyShiftLeft pyShiftLeft = new PyShiftLeft();
            pyShiftLeft.setEditor(getEdit());
            pyShiftLeft.run(null);

        } finally {

            if (d instanceof IDocumentExtension4) {
                IDocumentExtension4 extension = (IDocumentExtension4) d;
                extension.stopRewriteSession(rewriteSession);
            }

            if (fUndoManager != null) {
                fUndoManager.endCompoundChange();
            }
        }
    }

    private PyAutoIndentStrategy pyAutoIndentStrategy;

    /**
     * Overridden because we want to do things differently in block selection mode.
     */
    @Override
    protected void customizeDocumentCommand(DocumentCommand command) {
        if (pyAutoIndentStrategy == null) {
            pyAutoIndentStrategy = this.getEdit().getAutoEditStrategy();
        }

        boolean blockSelection = false;
        try {
            blockSelection = this.getTextWidget().getBlockSelection();
        } catch (Throwable e) {
            //that's OK (only available in eclipse 3.5)
        }

        pyAutoIndentStrategy.setBlockSelection(blockSelection);
        super.customizeDocumentCommand(command);
    }

    public Object getAdapter(Class adapter) {
        PyEdit pyEdit = projection.get();
        if (pyEdit != null) {
            return pyEdit.getAdapter(adapter);
        }
        return null;
    }

}