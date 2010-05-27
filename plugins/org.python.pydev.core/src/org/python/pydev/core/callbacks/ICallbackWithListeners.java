package org.python.pydev.core.callbacks;

public interface ICallbackWithListeners {

	Object call(Object obj);

	void registerListener(ICallbackListener listener);
}