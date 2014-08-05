/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.python.pydev.core.docutils.PythonPairMatcher;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.parsing.IParserObserver;
import org.python.pydev.shared_ui.editor.BaseEditor;

/**
 * @author Fabio Zadrozny
 *
 * The code below has been implemented after the following build notes:
 *
 * http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/buildnotes/buildnotes_text.html
 */
public abstract class PyEditProjection extends BaseEditor implements IParserObserver {

    private ProjectionSupport fProjectionSupport;

    public static final int PROP_FOLDING_CHANGED = -999;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, int)
     */
    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        IOverviewRuler overviewRuler = getOverviewRuler();
        PySourceViewer viewer = new PySourceViewer(parent, ruler, overviewRuler, isOverviewRulerVisible(), styles, this);

        //ensure decoration support has been created and configured.
        getSourceViewerDecorationSupport(viewer);

        return viewer;
    }

    public PySourceViewer getPySourceViewer() {
        return (PySourceViewer) getSourceViewer();
    }

    protected final static char[] BRACKETS = { '{', '}', '(', ')', '[', ']' };

    protected PythonPairMatcher fBracketMatcher = new PythonPairMatcher(BRACKETS);

    @Override
    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
        super.configureSourceViewerDecorationSupport(support);
        support.setCharacterPairMatcher(fBracketMatcher);
        support.setMatchingCharacterPainterPreferenceKeys(PydevEditorPrefs.USE_MATCHING_BRACKETS,
                PydevEditorPrefs.MATCHING_BRACKETS_COLOR);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        try {
            ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();

            fProjectionSupport = new ProjectionSupport(projectionViewer, getAnnotationAccess(), getSharedColors());
            fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error");
            fProjectionSupport.addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning");
            fProjectionSupport.setHoverControlCreator(new IInformationControlCreator() {
                public IInformationControl createInformationControl(Shell shell) {
                    return new DefaultInformationControl(shell);
                }
            });
            fProjectionSupport.install();

            if (isFoldingEnabled()) {
                projectionViewer.doOperation(ProjectionViewer.TOGGLE);
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * @return
     */
    public static boolean isFoldingEnabled() {
        return PydevPrefs.getPreferences().getBoolean(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
    }

    @Override
    public Object getAdapter(Class required) {
        if (fProjectionSupport != null) {
            Object adapter = fProjectionSupport.getAdapter(getSourceViewer(), required);
            if (adapter != null) {
                return adapter;
            }
        }

        return super.getAdapter(required);
    }

    /**
     * Sets the given message as error message to this editor's status line.
     *
     * @param msg message to be set
     */
    @Override
    public void setStatusLineErrorMessage(String msg) {
        IEditorStatusLine statusLine = (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
        if (statusLine != null) {
            statusLine.setMessage(true, msg, null);
        }
    }

}