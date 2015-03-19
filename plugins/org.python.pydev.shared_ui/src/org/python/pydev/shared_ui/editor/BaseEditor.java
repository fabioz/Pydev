/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.editor;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.python.pydev.overview_ruler.MinimapOverviewRuler;
import org.python.pydev.overview_ruler.MinimapOverviewRulerPreferencesPage;
import org.python.pydev.shared_core.editor.IBaseEditor;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.model.ErrorDescription;
import org.python.pydev.shared_core.model.IModelListener;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.parsing.BaseParserManager;
import org.python.pydev.shared_core.parsing.IScopesParser;
import org.python.pydev.shared_core.string.ICharacterPairMatcher2;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.OrderedSet;
import org.python.pydev.shared_core.utils.Reflection;
import org.python.pydev.shared_ui.outline.IOutlineModel;

public abstract class BaseEditor extends TextEditor implements IBaseEditor {

    /**
     * Those are the ones that register at runtime (not through extensions points).
     */
    protected final Collection<IPyEditListener> registeredEditListeners = new OrderedSet<IPyEditListener>();

    /**
     * Lock for initialization sync
     */
    private final Object initFinishedLock = new Object();

    /**
     * Indicates whether the init was already finished
     */
    protected boolean initFinished = false;

    protected final PyEditNotifier notifier;

    public BaseEditor() {
        super();
        notifier = new PyEditNotifier(this);

        try {
            //Applying the fix from https://bugs.eclipse.org/bugs/show_bug.cgi?id=368354#c18 in PyDev
            Field field = AbstractTextEditor.class.getDeclaredField("fSelectionChangedListener");
            field.setAccessible(true);
            field.set(this, new ISelectionChangedListener() {

                private Runnable fRunnable = new Runnable() {
                    public void run() {
                        ISourceViewer sourceViewer = BaseEditor.this.getSourceViewer();
                        // check whether editor has not been disposed yet
                        if (sourceViewer != null && sourceViewer.getDocument() != null) {
                            updateSelectionDependentActions();
                        }
                    }
                };

                private Display fDisplay;

                public void selectionChanged(SelectionChangedEvent event)
                {
                    Display current = Display.getCurrent();
                    if (current != null)
                    {
                        // Don't execute asynchronously if we're in a thread that has a display.
                        // Fix for: https://bugs.eclipse.org/bugs/show_bug.cgi?id=368354 (the rationale
                        // is that the actions were not being enabled because they were previously
                        // updated in an async call).
                        // but just patching getSelectionChangedListener() properly.
                        fRunnable.run();
                    }
                    else
                    {
                        if (fDisplay == null)
                        {
                            fDisplay = getSite().getShell().getDisplay();
                        }
                        fDisplay.asyncExec(fRunnable);
                    }
                    handleCursorPositionChanged();
                }
            });
        } catch (Exception e) {
            Log.log(e);
        }

    }

    public void addPyeditListener(IPyEditListener listener) {
        synchronized (registeredEditListeners) {
            registeredEditListeners.add(listener);
        }
    }

    public void removePyeditListener(IPyEditListener listener) {
        synchronized (registeredEditListeners) {
            registeredEditListeners.remove(listener);
        }
    }

    public List<IPyEditListener> getAllListeners() {
        return getAllListeners(true);
    }

    public List<IPyEditListener> getAllListeners(boolean waitInit) {
        if (waitInit) {
            while (initFinished == false) {
                synchronized (getInitFinishedLock()) {
                    try {
                        if (initFinished == false) {
                            getInitFinishedLock().wait();
                        }
                    } catch (Exception e) {
                        //ignore
                        Log.log(e);
                    }
                }
            }
        }
        ArrayList<IPyEditListener> listeners = new ArrayList<IPyEditListener>();
        List<IPyEditListener> editListeners = getAdditionalEditorListeners();
        if (editListeners != null) {
            listeners.addAll(editListeners); //no need to sync because editListeners is read-only
        }
        synchronized (registeredEditListeners) {
            listeners.addAll(registeredEditListeners);
        }
        return listeners;
    }

    protected List<IPyEditListener> getAdditionalEditorListeners() {
        return null;
    }

    protected Object getInitFinishedLock() {
        return initFinishedLock;
    }

    /**
     * Subclasses MUST call this method when the #init finishes.
     */
    protected void markInitFinished() {
        initFinished = true;
        synchronized (getInitFinishedLock()) {
            getInitFinishedLock().notifyAll();
        }
    }

    /**
     * implementation copied from org.eclipse.ui.externaltools.internal.ant.editor.PlantyEditor#setSelection
     */
    public void setSelection(int offset, int length) {
        ISourceViewer sourceViewer = getSourceViewer();
        sourceViewer.setSelectedRange(offset, length);
        sourceViewer.revealRange(offset, length);
    }

    public ISourceViewer getEditorSourceViewer() {
        return getSourceViewer();
    }

    public IAnnotationModel getAnnotationModel() {
        final IDocumentProvider documentProvider = getDocumentProvider();
        if (documentProvider == null) {
            return null;
        }
        return documentProvider.getAnnotationModel(getEditorInput());
    }

    /**
     * This map may be used by clients to store info regarding this editor.
     *
     * Clients should be careful so that this key is unique and does not conflict with other
     * plugins.
     *
     * This is not enforced.
     *
     * The suggestion is that the cache key is always preceded by the class name that will use it.
     */
    public Map<String, Object> cache = new HashMap<String, Object>();

    public Map<String, Object> getCache() {
        return cache;
    }

    public abstract void revealModelNodes(ISimpleNode[] node);

    /**
     * @return true if the editor passed as a parameter has the same input as this editor.
     */
    public boolean hasSameInput(IBaseEditor edit) {
        IEditorInput thisInput = this.getEditorInput();
        IEditorInput otherInput = (IEditorInput) edit.getEditorInput();
        if (thisInput == null || otherInput == null) {
            return false;
        }

        if (thisInput == otherInput || thisInput.equals(otherInput)) {
            return true;
        }

        IResource r1 = (IResource) thisInput.getAdapter(IResource.class);
        IResource r2 = (IResource) otherInput.getAdapter(IResource.class);
        if (r1 == null || r2 == null) {
            return false;
        }
        if (r1.equals(r2)) {
            return true;
        }
        return false;
    }

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        super.doSetInput(input);
    }

    @Override
    protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
        super.performSave(overwrite, progressMonitor);
        try {
            getParserManager().notifySaved(this);
            notifier.notifyOnSave();
        } catch (Throwable e) {
            //can never fail
            Log.log(e);
        }

    }

    @Override
    protected void createNavigationActions() {
        super.createNavigationActions();

        BaseEditorCursorListener cursorListener = new BaseEditorCursorListener(this);

        //add a cursor listener
        StyledText textWidget = getSourceViewer().getTextWidget();

        textWidget.addMouseListener(cursorListener);
        textWidget.addKeyListener(cursorListener);

    }

    /**
     * @return the document that is binded to this editor (may be null)
     */
    public IDocument getDocument() {
        IDocumentProvider documentProvider = getDocumentProvider();
        if (documentProvider != null) {
            return documentProvider.getDocument(getEditorInput());
        }
        return null;
    }

    /**
     * @return the project for the file that's being edited (or null if not available)
     */
    public IProject getProject() {
        IEditorInput editorInput = this.getEditorInput();
        if (editorInput instanceof IAdaptable) {
            IAdaptable adaptable = editorInput;
            IFile file = (IFile) adaptable.getAdapter(IFile.class);
            if (file != null) {
                return file.getProject();
            }
            IResource resource = (IResource) adaptable.getAdapter(IResource.class);
            if (resource != null) {
                return resource.getProject();
            }
            if (editorInput instanceof IStorageEditorInput) {
                IStorageEditorInput iStorageEditorInput = (IStorageEditorInput) editorInput;
                try {
                    IStorage storage = iStorageEditorInput.getStorage();
                    IPath fullPath = storage.getFullPath();
                    if (fullPath != null) {
                        IWorkspace ws = ResourcesPlugin.getWorkspace();
                        for (String s : fullPath.segments()) {
                            IProject p = ws.getRoot().getProject(s);
                            if (p.exists()) {
                                return p;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }

            }
        }
        return null;
    }

    /**
     * @return the IFile being edited in this input (or null if not available)
     */
    public IFile getIFile() {
        try {
            IEditorInput editorInput = this.getEditorInput();
            return (IFile) editorInput.getAdapter(IFile.class);
        } catch (Exception e) {
            Log.log(e); //Shouldn't really happen, but if it does, let's not fail!
            return null;
        }
    }

    /**
     * @return the File being edited
     */
    public File getEditorFile() {
        File f = null;
        IEditorInput editorInput = this.getEditorInput();
        IFile file = (IFile) editorInput.getAdapter(IFile.class);
        if (file != null) {
            IPath location = file.getLocation();
            if (location != null) {
                IPath path = location.makeAbsolute();
                f = path.toFile();
            }

        } else {
            try {
                if (editorInput instanceof IURIEditorInput) {
                    IURIEditorInput iuriEditorInput = (IURIEditorInput) editorInput;
                    return new File(iuriEditorInput.getURI());
                }
            } catch (Throwable e) {
                //OK, IURIEditorInput was only added on eclipse 3.3
            }

            try {
                IPath path = (IPath) Reflection.invoke(editorInput, "getPath", new Object[0]);
                f = path.toFile();
            } catch (Throwable e) {
                //ok, it has no getPath
            }
        }
        return f;
    }

    protected abstract BaseParserManager getParserManager();

    /** listeners that get notified of model changes */
    protected final List<IModelListener> modelListeners = new ArrayList<IModelListener>();

    /** stock listener implementation */
    public void addModelListener(IModelListener listener) {
        Assert.isNotNull(listener);
        if (!modelListeners.contains(listener)) {
            modelListeners.add(listener);
        }
    }

    /** stock listener implementation */
    public void removeModelListener(IModelListener listener) {
        Assert.isNotNull(listener);
        modelListeners.remove(listener);
    }

    /**
     * stock listener implementation event is fired whenever we get a new root
     */
    protected void fireModelChanged(ISimpleNode root) {
        //create a copy, to avoid concurrent modifications
        for (IModelListener listener : new ArrayList<IModelListener>(modelListeners)) {
            try {
                listener.modelChanged(root);
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * stock listener implementation event is fired whenever the errors change in the editor
     */
    protected void fireParseErrorChanged(ErrorDescription errorDesc) {
        for (IModelListener listener : new ArrayList<IModelListener>(modelListeners)) {
            listener.errorChanged(errorDesc);
        }
    }

    public abstract TextSelectionUtils createTextSelectionUtils();

    /**
     * Notifies clients about a change in the cursor position.
     */
    public void notifyCursorPositionChanged() {
        if (!this.initFinished) {
            return;
        }
        TextSelectionUtils ps = createTextSelectionUtils();
        for (IPyEditListener listener : this.getAllListeners()) {
            try {
                if (listener instanceof IPyEditListener2) {
                    ((IPyEditListener2) listener).handleCursorPositionChanged(this, ps);
                }
            } catch (Throwable e) {
                //must not fail
                Log.log(e);
            }
        }
    }

    public abstract ICharacterPairMatcher2 getPairMatcher();

    public abstract IScopesParser createScopesParser();

    @Override
    protected IOverviewRuler createOverviewRuler(ISharedTextColors sharedColors) {
        // Note: create the minimap overview ruler regardless of whether it should be shown or not
        // (the setting to show it will control what's drawn).
        if (MinimapOverviewRulerPreferencesPage.useMinimap()) {
            IOutlineModel outlineModel = (IOutlineModel) this.getAdapter(IOutlineModel.class);
            IOverviewRuler ruler = new MinimapOverviewRuler(getAnnotationAccess(), sharedColors, outlineModel);

            Iterator e = getAnnotationPreferences().getAnnotationPreferences().iterator();
            while (e.hasNext()) {
                AnnotationPreference preference = (AnnotationPreference) e.next();
                if (preference.contributesToHeader()) {
                    ruler.addHeaderAnnotationType(preference.getAnnotationType());
                }
            }
            return ruler;
        } else {
            return super.createOverviewRuler(sharedColors);
        }
    }

    IOutlineModel outlineModel;

    @Override
    public Object getAdapter(Class adapter) {
        if (IOutlineModel.class.equals(adapter)) {
            if (outlineModel == null) {
                outlineModel = createOutlineModel();
            }
            return outlineModel;
        }
        return super.getAdapter(adapter);
    }

    public abstract IOutlineModel createOutlineModel();

    @Override
    public void dispose() {
        if (outlineModel != null) {
            outlineModel.dispose();
            outlineModel = null;
        }
        super.dispose();
    }

}
