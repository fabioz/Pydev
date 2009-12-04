package org.python.pydev.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.docutils.PyPartitionScanner;
import org.python.pydev.editor.PyEditConfigurationWithoutEditor;
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.ui.ColorCache;

public class PyMergeViewer extends TextMergeViewer {

    private ColorCache colorCache;


    public PyMergeViewer(Composite parent, int style, CompareConfiguration configuration) {
        super(parent, style | SWT.LEFT_TO_RIGHT, configuration);
    }

    @Override
    protected IDocumentPartitioner getDocumentPartitioner() {
        return PyPartitionScanner.createPyPartitioner();
    }

    /**
     * Overridden to handle the partitioning (will only work on Eclipse 3.3)
     */
    @Override
    protected String getDocumentPartitioning() {
        return IPythonPartitions.PYTHON_PARTITION_TYPE;
    }
    
    
    /**
     * Overridden to handle backspace (will only work on Eclipse 3.5)
     */
    @Override
    protected SourceViewer createSourceViewer(Composite parent, int textOrientation) {
        final SourceViewer viewer = super.createSourceViewer(parent, textOrientation);
        viewer.appendVerifyKeyListener(PyBackspace.createVerifyKeyListener(viewer, null));
        return viewer;
    }
    
    
    @Override
    protected void configureTextViewer(TextViewer textViewer) {
        if (!(textViewer instanceof SourceViewer))
            return;

        SourceViewerConfiguration sourceViewerConfiguration= new PyEditConfigurationWithoutEditor(
                this.getColorCache(), PydevPrefs.getChainedPrefStore());
        ((SourceViewer)textViewer).configure(sourceViewerConfiguration);
    }
    

    private ColorCache getColorCache() {
        if(this.colorCache == null){
            this.colorCache = new ColorCache(PydevPrefs.getChainedPrefStore());
        }
        return this.colorCache;
    }

    @Override
    protected void handleDispose(DisposeEvent event) {
        super.handleDispose(event);
        if(this.colorCache != null){
            this.colorCache.dispose();
            this.colorCache = null;
        }
    }

}
