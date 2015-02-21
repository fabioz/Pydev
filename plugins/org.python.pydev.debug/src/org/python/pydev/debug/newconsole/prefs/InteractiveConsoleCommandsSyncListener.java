package org.python.pydev.debug.newconsole.prefs;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.shared_ui.editor.IPyEditListener4;

public class InteractiveConsoleCommandsSyncListener implements IPyEditListener, IPyEditListener4 {

    @Override
    public void onSave(BaseEditor edit, IProgressMonitor monitor) {

    }

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onDispose(BaseEditor edit, IProgressMonitor monitor) {
    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onEditorCreated(BaseEditor baseEditor) {
        //When a PyDev editor is created, make sure that the bindings related to the commands set to the console are
        //kept updated.
        InteractiveConsoleCommand.keepBindingsUpdated();
    }

}
