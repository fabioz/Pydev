/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

public class PyUnitTestStarted {

    public final String location;
    public final String test;

    private WeakReference<PyUnitTestRun> testRun;

    public PyUnitTestStarted(PyUnitTestRun testRun, String location, String test) {
        this.testRun = new WeakReference<PyUnitTestRun>(testRun);
        this.location = location;
        this.test = test;
    }

    public PyUnitTestRun getTestRun() {
        return this.testRun.get();
    }

}
