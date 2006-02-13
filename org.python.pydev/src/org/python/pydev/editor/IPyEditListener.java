package org.python.pydev.editor;

public interface IPyEditListener {

	/**
	 * Anytime a PyEdit is saved, it will notify that to its listeners.
	 * @param edit the PyEdit that has just been saved.
	 */
	void onSave(PyEdit edit);
}
