/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.utils;

/**
 * This class is a helper in performance evaluation
 */
public class Timer {

    private long start;

    public Timer() {
        this.start = System.currentTimeMillis();
    }

    public void printDiffMillis() {
        System.out.println("Time Elapsed (millis):" + getDiff());
    }

    public void printDiff() {
        printDiff(null);
    }

    private long getDiff() {
        long old = this.start;
        long newStart = System.currentTimeMillis();
        long diff = (newStart - old);
        start = newStart;
        return diff;
    }

    public void printDiff(String msg) {
        double secs = getDiff() / 1000.0d;
        if (msg != null) {
            System.out.println("Time Elapsed for:" + msg + " (secs):" + secs);
        } else {
            System.out.println("Time Elapsed (secs):" + secs);
        }
    }
}
