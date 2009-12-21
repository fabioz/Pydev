package org.python.pydev.compare;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PyPartitionScanner;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.IPySyntaxHighlightingAndCodeCompletionEditor;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PyEditConfiguration;
import org.python.pydev.editor.PyEditConfigurationWithoutEditor;
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.ui.ColorCache;

/**
 * Provides a viewer that provides:
 * - syntax highlighting
 * - indent strategy
 * - proper backspace
 * - code completion
 */
public class PyMergeViewer extends TextMergeViewer {

    private List<ColorCache> colorCache = new ArrayList<ColorCache>();
    private List<IPropertyChangeListener> prefChangeListeners = new ArrayList<IPropertyChangeListener>();


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
    
    
    private IPythonNature getPythonNature(Object compareInput) {
        IResource resource = getResource(compareInput);
        if(resource != null){
            return PythonNature.getPythonNature(resource);
        }
        return null;
        
    }

    private IResource getResource(Object compareInput) {
        if (!(compareInput instanceof ICompareInput)) {
            return null;
        }
        ICompareInput input = (ICompareInput) compareInput;
        if (input == null){
            return null;
        }

        IResourceProvider rp= null;
        ITypedElement te= input.getLeft();
        
        if (te instanceof IResourceProvider){
            rp= (IResourceProvider) te;
        }
        
        if (rp == null) {
            te= input.getRight();
            if (te instanceof IResourceProvider){
                rp= (IResourceProvider) te;
            }
        }
        
        if (rp == null) {
            te= input.getAncestor();
            if (te instanceof IResourceProvider){
                rp= (IResourceProvider) te;
            }
        }
        if (rp != null) {
            return rp.getResource();
        }
        return null;
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
    
    
    @SuppressWarnings("unchecked")
    @Override
    protected void configureTextViewer(TextViewer textViewer) {
        if (!(textViewer instanceof SourceViewer))
            return;
        final SourceViewer sourceViewer = (SourceViewer) textViewer;

        final IIndentPrefs indentPrefs = new DefaultIndentPrefs();
        
        //Hack to provide the source viewer configuration that'll only be created later (there's a cycle there).
        final WeakReference<PyEditConfigurationWithoutEditor>[] sourceViewerConfigurationObj = new WeakReference[1];
        
        final ColorCache c = new ColorCache(PydevPrefs.getChainedPrefStore());
        this.colorCache.add(c); //add for it to be disposed later.
        
        IPySyntaxHighlightingAndCodeCompletionEditor editor = new IPySyntaxHighlightingAndCodeCompletionEditor() {
            
            public void resetForceTabs() {
                
            }
            
            public IIndentPrefs getIndentPrefs() {
                return indentPrefs;
            }
            
            public ISourceViewer getEditorSourceViewer() {
                return sourceViewer;
            }
            
            public PyEditConfigurationWithoutEditor getEditConfiguration() {
                return sourceViewerConfigurationObj[0].get();
            }
            
            public ColorCache getColorCache() {
                return c;
            }

            public PySelection createPySelection() {
                ISelection selection = sourceViewer.getSelection();
                if(selection instanceof ITextSelection){
                    return new PySelection(sourceViewer.getDocument(), (ITextSelection)selection);
                }else{
                    return null;
                }
            }

            public File getEditorFile() {
                IResource file = PyMergeViewer.this.getResource(PyMergeViewer.this.getInput());
                if (file != null && file instanceof IFile) {
                    IPath path = file.getLocation().makeAbsolute();
                    return path.toFile();
                }
                return null;
            }

            public IPythonNature getPythonNature() throws MisconfigurationException {
                return PyMergeViewer.this.getPythonNature(PyMergeViewer.this.getInput());
            }

            public Object getAdapter(Class adapter) {
                if(adapter == IResource.class){
                    return PyMergeViewer.this.getResource(PyMergeViewer.this.getInput());
                }
                if(adapter == IFile.class){
                    IResource resource = PyMergeViewer.this.getResource(PyMergeViewer.this.getInput());
                    if(resource instanceof IFile){
                        return resource;
                    }
                }
                return null;
            }
        };
        
        final PyEditConfiguration sourceViewerConfiguration= new PyEditConfiguration(
                c, editor, PydevPrefs.getChainedPrefStore());
        sourceViewerConfigurationObj[0] = new WeakReference<PyEditConfigurationWithoutEditor>(sourceViewerConfiguration);
        sourceViewer.configure(sourceViewerConfiguration);
        
        
        IPropertyChangeListener prefChangeListener = PyEdit.createPrefChangeListener(editor);
        prefChangeListeners.add(prefChangeListener);
        PydevPrefs.getPreferences().addPropertyChangeListener(prefChangeListener);
    }
    


    @Override
    protected void handleDispose(DisposeEvent event) {
        super.handleDispose(event);
        
        for(ColorCache c:colorCache){
            c.dispose();
        }
        this.colorCache.clear();
        
        for(IPropertyChangeListener l:prefChangeListeners){
            PydevPrefs.getPreferences().removePropertyChangeListener(l);
        }
        prefChangeListeners.clear();
    }

}
