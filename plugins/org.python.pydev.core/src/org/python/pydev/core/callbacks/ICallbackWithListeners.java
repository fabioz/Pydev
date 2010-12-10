package org.python.pydev.core.callbacks;

public interface ICallbackWithListeners<X> {

	Object call(X obj);

	void registerListener(ICallbackListener<X> listener);
}