package org.python.pydev.editor;

public interface IPyEditCallback {

	Object call(Object obj);

	void registerListener(IPyEditCallbackListener listener);
}