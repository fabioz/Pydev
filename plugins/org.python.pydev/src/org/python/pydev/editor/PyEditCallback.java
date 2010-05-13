package org.python.pydev.editor;

import java.util.ArrayList;

import org.python.pydev.plugin.PydevPlugin;

public class PyEditCallback implements IPyEditCallback{

	private ArrayList<IPyEditCallbackListener> listeners;

	public PyEditCallback() {
		this.listeners = new ArrayList<IPyEditCallbackListener>();
	}
	
	/* (non-Javadoc)
	 * @see com.aptana.editor.common.extensions.IThemeableEditorCallback#call(java.lang.Object)
	 */
	public Object call(Object obj) {
		Object result = null;
		for(IPyEditCallbackListener listener:this.listeners){
			try {
				Object callResult = listener.call(obj);
				if(callResult != null){
					result = callResult;
				}
			} catch (Throwable e) {
				//Should never fail!
				PydevPlugin.log(e);
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.aptana.editor.common.extensions.IThemeableEditorCallback#registerListener(com.aptana.editor.common.extensions.IPyEditCallbackListener)
	 */
	public void registerListener(IPyEditCallbackListener listener) {
		this.listeners.add(listener);
	}

}
