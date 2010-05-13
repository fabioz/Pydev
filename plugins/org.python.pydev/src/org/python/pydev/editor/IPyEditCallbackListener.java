package org.python.pydev.editor;


/**
 * A simple callback that will allow extensions to know about what they need to
 * hear from the editor.
 */
public interface IPyEditCallbackListener {

	Object call(Object obj);

}
