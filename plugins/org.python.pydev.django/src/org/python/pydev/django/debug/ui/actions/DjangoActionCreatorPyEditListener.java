/*
 * Created on May 21, 2006
 */
package org.python.pydev.django.debug.ui.actions;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;


/**
 * Creates any django-related actions for an editor.
 */
public class DjangoActionCreatorPyEditListener implements IPyEditListener{

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
        edit.addOfflineActionListener("dj", new PyDjangoOfflineAction(edit), "Execute django action", true);
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
    }

}
