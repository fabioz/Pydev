package org.python.pydev.shared_core.out_of_memory;

import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.log.Log;

public class OnExpectedOutOfMemory {

    public static final CallbackWithListeners<Object> clearCacheOnOutOfMemory = new CallbackWithListeners<>();

    static {
        clearCacheOnOutOfMemory.registerListener(new ICallbackListener<Object>() {

            @Override
            public Object call(Object obj) {
                Log.logWarning("Low memory detected on JVM: Clearing caches (consider raising -Xmx setting on .ini)");
                return null;
            }
        });
    }

}
