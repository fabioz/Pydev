package org.python.pydev.editor;

import java.util.ListResourceBundle;

import org.eclipse.jface.text.IDocument;


public interface IPyEditListener {

	/**
	 * Anytime a PyEdit is saved, it will notify that to its listeners.
	 * @param edit the PyEdit that has just been saved.
	 */
	void onSave(PyEdit edit);

    /**
     * When the actions are being created in PyEdit, this method is called, so that contributors might add their own actions 
     * @param resources the resource bundle it used
     * @param edit the PyEdit
     */
    void onCreateActions(ListResourceBundle resources, PyEdit edit);

    /**
     * This method is called whenever the edit is disposed
     * @param edit the edit that will be disposed.
     */
    void onDispose(PyEdit edit);

    void onSetDocument(IDocument document, PyEdit edit);
}
