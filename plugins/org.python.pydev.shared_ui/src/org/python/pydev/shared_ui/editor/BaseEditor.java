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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
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
        if (editorInput instanceof FileEditorInput) {
            IFile file = (IFile) ((FileEditorInput) editorInput).getAdapter(IFile.class);
            return file.getProject();
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
}
