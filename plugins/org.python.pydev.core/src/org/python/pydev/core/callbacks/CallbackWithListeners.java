package org.python.pydev.core.callbacks;

import java.util.ArrayList;

import org.python.pydev.core.log.Log;

public class CallbackWithListeners implements ICallbackWithListeners{

	private ArrayList<ICallbackListener> listeners;

	public CallbackWithListeners() {
		this.listeners = new ArrayList<ICallbackListener>();
	}
	
	public Object call(Object obj) {
		Object result = null;
		for(ICallbackListener listener:this.listeners){
			try {
				Object callResult = listener.call(obj);
				if(callResult != null){
					result = callResult;
				}
			} catch (Throwable e) {
				//Should never fail!
				Log.log(e);
			}
		}
		return result;
	}

	public void registerListener(ICallbackListener listener) {
		this.listeners.add(listener);
	}

}
