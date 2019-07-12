package org.python.pydev.core.editor;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.shared_core.callbacks.ICallback;

public class OpenEditors {

    static private final Set<IPyEdit> currentlyOpenedEditors = new HashSet<IPyEdit>();
    static private final Object currentlyOpenedEditorsLock = new Object();

    public static boolean isEditorOpenForResource(IResource r) {
        HashSet<IPyEdit> hashSet;
        synchronized (currentlyOpenedEditorsLock) {
            hashSet = new HashSet<>(currentlyOpenedEditors);
        }
        // Iterate in unsynchronized copy
        for (IPyEdit edit : hashSet) {
            IAdaptable input = edit.getEditorInput();
            if (input != null) {
                Object adapter = input.getAdapter(IResource.class);
                if (adapter != null && r.equals(adapter)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void removeOpenedEditor(IPyEdit pyEdit) {
        synchronized (currentlyOpenedEditorsLock) {
            currentlyOpenedEditors.remove(pyEdit);
        }
    }

    public static Object iterOpenEditorsUntilFirstReturn(ICallback<Object, IPyEdit> callback) {
        HashSet<IPyEdit> hashSet;
        synchronized (currentlyOpenedEditorsLock) {
            hashSet = new HashSet<>(currentlyOpenedEditors);
        }
        // Iterate in unsynchronized copy
        for (IPyEdit edit : hashSet) {
            Object ret = callback.call(edit);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static void addOpenedEditor(IPyEdit pyEdit) {
        synchronized (currentlyOpenedEditorsLock) {
            currentlyOpenedEditors.add(pyEdit);
        }
    }
}
