package org.python.pydev.debug.newconsole.prefs;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.debug.newconsole.prefs.InteractiveConsoleCommand.InteractiveCommandCustomHandler;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.shared_ui.editor.IPyEditListener4;

public class InteractiveConsoleCommandsSyncListener implements IPyEditListener, IPyEditListener4 {

    private static final class InteractiveConsoleUserCommandAction extends TextEditorAction {
        private String commandId;

        private InteractiveConsoleUserCommandAction(ResourceBundle bundle, String prefix, ITextEditor editor,
                String commandId) {
            super(bundle, prefix, editor);
            this.commandId = commandId;
        }

        @Override
        public void run() {
            ITextEditor textEditor = this.getTextEditor();
            if (textEditor instanceof PyEdit) {
                PyEdit pyEdit = (PyEdit) textEditor;
                Map<String, InteractiveCommandCustomHandler> commandIdToHandler = InteractiveConsoleCommand
                        .getCommandIdToHandler();
                InteractiveCommandCustomHandler iHandler = commandIdToHandler.get(this.commandId);
                if (iHandler != null) {
                    iHandler.execute(pyEdit);
                }
            }
        }
    }

    private ListResourceBundle resources;
    private WeakReference<BaseEditor> weakEditor;
    private Map<String, InteractiveConsoleUserCommandAction> idToAction = new HashMap<String, InteractiveConsoleCommandsSyncListener.InteractiveConsoleUserCommandAction>();
    private final ICallbackListener<Object> onCommandIdToHandlerChangedCallback = new ICallbackListener<Object>() {

        @Override
        public Object call(Object obj) {
            BaseEditor baseEditor = weakEditor.get();
            if (baseEditor == null) {
                InteractiveConsoleCommand.onCommandIdToHandlerChanged.unregisterListener(this);
            }
            onCreateActions(resources, baseEditor, null);
            return null;
        }
    };

    @Override
    public void onSave(BaseEditor edit, IProgressMonitor monitor) {

    }

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
        //Note: can be called multiple times (will only create handlers for the new actions).
        Map<String, InteractiveCommandCustomHandler> commandIdToHandler = InteractiveConsoleCommand
                .getCommandIdToHandler();
        Set<Entry<String, InteractiveCommandCustomHandler>> entrySet = commandIdToHandler.entrySet();
        for (Entry<String, InteractiveCommandCustomHandler> entry : entrySet) {
            if (idToAction.containsKey(entry.getKey())) {
                continue;
            }
            InteractiveConsoleUserCommandAction action = new InteractiveConsoleUserCommandAction(resources,
                    "Pyedit.InteractiveConsoleUserCommand", baseEditor, entry.getKey());
            action.setActionDefinitionId(entry.getKey());
            action.setId(entry.getKey());
            baseEditor.setAction(FullRepIterable.getLastPart(entry.getKey()), action);
            idToAction.put(entry.getKey(), action);
        }
        this.resources = resources;
        this.weakEditor = new WeakReference<>(baseEditor);
        InteractiveConsoleCommand.onCommandIdToHandlerChanged.registerListener(onCommandIdToHandlerChangedCallback);
    }

    @Override
    public void onDispose(BaseEditor edit, IProgressMonitor monitor) {
        InteractiveConsoleCommand.onCommandIdToHandlerChanged.unregisterListener(onCommandIdToHandlerChangedCallback);
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
