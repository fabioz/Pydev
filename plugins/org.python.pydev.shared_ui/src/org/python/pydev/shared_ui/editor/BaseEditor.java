package org.python.pydev.shared_ui.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextEditor;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.structure.OrderedSet;

public abstract class BaseEditor extends TextEditor {

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

    public abstract void revealModelNodes(ISimpleNode[] node);
}
