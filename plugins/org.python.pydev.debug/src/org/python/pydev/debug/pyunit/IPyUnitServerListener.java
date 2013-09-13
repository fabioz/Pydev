/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

public interface IPyUnitServerListener {

    void notifyTestsCollected(String totalTestsCount);

    void notifyTest(String status, String location, String test, String capturedOutput, String errorContents,
            String time);

    void notifyDispose();

    void notifyFinished(String totalTimeInSecs);

    void notifyStartTest(String location, String test);

}
