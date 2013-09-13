/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.utils;

import org.python.pydev.core.log.Log;

/**
 * This is an auxiliary class for things that require callbacks at some interval.
 * 
 * It allows you to specify the number of times that the callback should be called.  
 *
 */
public class CounterThread extends Thread {
    private ICallback callback;
    private int elapseTime;
    private int stopWhenReaches;

    /**
     * @param callback the callback that should be called whenever the time elapses (it passes an int, that
     * says which call is this one).
     * @param elapseTime this is the millis that should elapse between the calls
     * @param stopWhenReaches this is the number of times that it should be called
     */
    public CounterThread(ICallback callback, int elapseTime, int stopWhenReaches) {
        this.callback = callback;
        this.elapseTime = elapseTime;
        this.stopWhenReaches = stopWhenReaches;
        setName("Callback (CounterThread)");
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < stopWhenReaches; i++) {
                try {
                    sleep(elapseTime);
                    callback.call(i);
                } catch (Exception e) {
                    Log.log(e);
                    return;
                }
            }

        } catch (Exception e) {
            Log.log(e);
            return;
        }
    }
}
