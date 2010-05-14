package org.python.pydev.core.callbacks;


/**
 * A simple callback that will allow extensions to know about what they need.
 */
public interface ICallbackListener {

	Object call(Object obj);

}
