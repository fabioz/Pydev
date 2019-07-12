package org.python.pydev.shared_core.resources;

import java.lang.ref.WeakReference;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.python.pydev.shared_core.cache.LRUMap;
import org.python.pydev.shared_core.structure.Tuple3;

public class DocumentChanged {

    public static final Map<IFile, Tuple3<Long, Long, WeakReference<IDocument>>> fileToSavedTime = new LRUMap<>(20);

    public static void markSavedTimes(IFile file, IDocument document) {
        fileToSavedTime.put(file, new Tuple3<Long, Long, WeakReference<IDocument>>(
                ((IDocumentExtension4) document).getModificationStamp(), file.getModificationStamp(),
                new WeakReference<>(document)));
    }

    public static boolean hasDocumentChanged(IResource resource, IDocument document) {
        Tuple3<Long, Long, WeakReference<IDocument>> tuple = fileToSavedTime.get(resource);
        if (tuple == null) {
            return false;
        }
        IDocument cachedDoc = tuple.o3.get();
        if (cachedDoc == document) {
            if (((IDocumentExtension4) document).getModificationStamp() == tuple.o1) {
                return false;
            }
            return true;
        }
        return false;
    }

}
