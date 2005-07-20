/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.python.copiedfromeclipsesrc.PythonPairMatcher;
import org.python.pydev.parser.IParserListener;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

/**
 * @author Fabio Zadrozny
 * 
 * The code below has been implemented after the following build notes:
 * 
 * http://download2.eclipse.org/downloads/drops/S-3.0M9-200405211200/buildnotes/buildnotes_text.html
 */
public abstract class PyEditProjection extends TextEditor implements IParserListener {

    private ProjectionSupport fProjectionSupport;

    public static final int PROP_FOLDING_CHANGED = -999;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, int)
     */
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        IOverviewRuler overviewRuler = getOverviewRuler();
        PySourceViewer viewer = new PySourceViewer(parent, ruler, overviewRuler, isOverviewRulerVisible(), styles, this);
        
        //ensure decoration support has been created and configured.
        getSourceViewerDecorationSupport(viewer);

        return viewer;
    }
    

    /**
     * @return a preference store that has the pydev preference store and the default editors text store
     */
    protected IPreferenceStore getChainedPrefStore() {
        IPreferenceStore general = EditorsUI.getPreferenceStore();
        IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
        ChainedPreferenceStore store = new ChainedPreferenceStore(new IPreferenceStore[] { general, preferenceStore });
        return store;
    }


    protected final static char[] BRACKETS = { '{', '}', '(', ')', '[', ']' };

    protected PythonPairMatcher fBracketMatcher = new PythonPairMatcher(BRACKETS);

    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
        super.configureSourceViewerDecorationSupport(support);
        support.setCharacterPairMatcher(fBracketMatcher);
        support.setMatchingCharacterPainterPreferenceKeys(PydevPrefs.USE_MATCHING_BRACKETS, PydevPrefs.MATCHING_BRACKETS_COLOR);
    }

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
            e.printStackTrace();
        }
    }

    /**
     * @return
     */
    public static boolean isFoldingEnabled() {
        return PydevPrefs.getPreferences().getBoolean(PydevPrefs.USE_CODE_FOLDING);
    }

    public Object getAdapter(Class required) {
        if (fProjectionSupport != null) {
            Object adapter = fProjectionSupport.getAdapter(getSourceViewer(), required);
            if (adapter != null)
                return adapter;
        }

        return super.getAdapter(required);
    }

    /**
     * Sets the given message as error message to this editor's status line.
     * 
     * @param msg message to be set
     */
    public void setStatusLineErrorMessage(String msg) {
        IEditorStatusLine statusLine = (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
        if (statusLine != null)
            statusLine.setMessage(true, msg, null);
    }

}