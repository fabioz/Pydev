/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.editor.IPySyntaxHighlightingAndCodeCompletionEditor;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PyEditConfiguration;
import org.python.pydev.editor.PyEditConfigurationWithoutEditor;
import org.python.pydev.editor.actions.FirstCharAction;
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.editor.actions.PyPeerLinker;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.ui.ColorAndStyleCache;

/**
 * Provides a viewer that provides:
 * - syntax highlighting
 * - indent strategy
 * - proper backspace
 * - code completion
 */
public class PyMergeViewer extends TextMergeViewer {

    private List<ColorAndStyleCache> colorCache;
    private List<IPropertyChangeListener> prefChangeListeners;

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
        if (resource != null) {
            return PythonNature.getPythonNature(resource);
        }
        return null;

    }

    private IResource getResource(Object compareInput) {
        if (!(compareInput instanceof ICompareInput)) {
            return null;
        }
        ICompareInput input = (ICompareInput) compareInput;

        IResourceProvider rp = null;
        ITypedElement te = input.getLeft();

        if (te instanceof IResourceProvider) {
            rp = (IResourceProvider) te;
        }

        if (rp == null) {
            te = input.getRight();
            if (te instanceof IResourceProvider) {
                rp = (IResourceProvider) te;
            }
        }

        if (rp == null) {
            te = input.getAncestor();
            if (te instanceof IResourceProvider) {
                rp = (IResourceProvider) te;
            }
        }
        if (rp != null) {
            return rp.getResource();
        }
        return null;
    }

    /**
     * Overridden to handle backspace (will only be called on Eclipse 3.5)
     */
    @Override
    protected SourceViewer createSourceViewer(Composite parent, int textOrientation) {
        final SourceViewer viewer = super.createSourceViewer(parent, textOrientation);
        viewer.appendVerifyKeyListener(PyPeerLinker.createVerifyKeyListener(viewer));
        viewer.appendVerifyKeyListener(PyBackspace.createVerifyKeyListener(viewer, null));
        IWorkbenchPart workbenchPart = getCompareConfiguration().getContainer().getWorkbenchPart();

        //Note that any site should be OK as it's just to know if a keybinding is active. 
        IWorkbenchPartSite site = null;
        if (workbenchPart != null) {
            site = workbenchPart.getSite();
        } else {
            IWorkbenchWindow window = EditorUtils.getActiveWorkbenchWindow();
            if (window != null) {
                IWorkbenchPage activePage = window.getActivePage();
                if (activePage != null) {
                    IWorkbenchPart activePart = activePage.getActivePart();
                    if (activePart != null) {
                        site = activePart.getSite();
                    }
                }
            }
        }
        VerifyKeyListener createVerifyKeyListener = FirstCharAction.createVerifyKeyListener(viewer, site, true);
        if (createVerifyKeyListener != null) {
            viewer.appendVerifyKeyListener(createVerifyKeyListener);
        }
        return viewer;
    }

    private List<ColorAndStyleCache> getColorCache() {
        if (this.colorCache == null) {
            this.colorCache = new ArrayList<ColorAndStyleCache>();
        }
        return this.colorCache;
    }

    public List<IPropertyChangeListener> getPrefChangeListeners() {
        if (this.prefChangeListeners == null) {
            this.prefChangeListeners = new ArrayList<IPropertyChangeListener>();
        }
        return this.prefChangeListeners;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configureTextViewer(TextViewer textViewer) {
        if (!(textViewer instanceof SourceViewer)) {
            return;
        }
        final SourceViewer sourceViewer = (SourceViewer) textViewer;

        IAdaptable adaptable;
        if (sourceViewer instanceof IAdaptable) {
            adaptable = (IAdaptable) sourceViewer;
        } else {
            adaptable = new IAdaptable() {

                @Override
                public Object getAdapter(Class adapter) {
                    return null;
                }
            };
        }

        final IIndentPrefs indentPrefs = new DefaultIndentPrefs(adaptable);

        //Hack to provide the source viewer configuration that'll only be created later (there's a cycle there).
        final WeakReference<PyEditConfigurationWithoutEditor>[] sourceViewerConfigurationObj = new WeakReference[1];

        IPreferenceStore chainedPrefStore = PydevPrefs.getChainedPrefStore();
        final ColorAndStyleCache c = new ColorAndStyleCache(chainedPrefStore);
        this.getColorCache().add(c); //add for it to be disposed later.

        IPySyntaxHighlightingAndCodeCompletionEditor editor = new IPySyntaxHighlightingAndCodeCompletionEditor() {

            public void resetForceTabs() {

            }

            public void resetIndentPrefixes() {
                SourceViewerConfiguration configuration = getEditConfiguration();
                String[] types = configuration.getConfiguredContentTypes(sourceViewer);
                for (int i = 0; i < types.length; i++) {
                    String[] prefixes = configuration.getIndentPrefixes(sourceViewer, types[i]);
                    if (prefixes != null && prefixes.length > 0) {
                        sourceViewer.setIndentPrefixes(prefixes, types[i]);
                    }
                }
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

            public ColorAndStyleCache getColorCache() {
                return c;
            }

            public PySelection createPySelection() {
                ISelection selection = sourceViewer.getSelection();
                if (selection instanceof ITextSelection) {
                    return new PySelection(sourceViewer.getDocument(), (ITextSelection) selection);
                } else {
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

            @Override
            public int getGrammarVersion() throws MisconfigurationException {
                IPythonNature pythonNature = this.getPythonNature();
                if (pythonNature == null) {
                    Log.logInfo("Expected to get the PythonNature at this point...");
                    return IGrammarVersionProvider.LATEST_GRAMMAR_VERSION;
                }
                return pythonNature.getGrammarVersion();
            }

            public Object getAdapter(Class adapter) {
                if (adapter == IResource.class) {
                    return PyMergeViewer.this.getResource(PyMergeViewer.this.getInput());
                }
                if (adapter == IFile.class) {
                    IResource resource = PyMergeViewer.this.getResource(PyMergeViewer.this.getInput());
                    if (resource instanceof IFile) {
                        return resource;
                    }
                }
                if (adapter == IProject.class) {
                    IResource resource = PyMergeViewer.this.getResource(PyMergeViewer.this.getInput());
                    if (resource instanceof IFile) {
                        return resource.getProject();
                    }
                }
                return null;
            }
        };

        final PyEditConfiguration sourceViewerConfiguration = new PyEditConfiguration(c, editor, chainedPrefStore);
        sourceViewerConfiguration.getPyAutoIndentStrategy(editor); // Force its initialization
        sourceViewerConfigurationObj[0] = new WeakReference<PyEditConfigurationWithoutEditor>(sourceViewerConfiguration);
        sourceViewer.configure(sourceViewerConfiguration);

        IPropertyChangeListener prefChangeListener = PyEdit.createPrefChangeListener(editor);
        getPrefChangeListeners().add(prefChangeListener);
        chainedPrefStore.addPropertyChangeListener(prefChangeListener);
    }

    @Override
    protected void handleDispose(DisposeEvent event) {
        super.handleDispose(event);

        List<ColorAndStyleCache> colorCache = getColorCache();
        for (ColorAndStyleCache c : colorCache) {
            c.dispose();
        }
        colorCache.clear();

        List<IPropertyChangeListener> prefChangeListeners = getPrefChangeListeners();
        for (IPropertyChangeListener l : prefChangeListeners) {
            PydevPrefs.getChainedPrefStore().removePropertyChangeListener(l);
        }
        prefChangeListeners.clear();
    }

}
