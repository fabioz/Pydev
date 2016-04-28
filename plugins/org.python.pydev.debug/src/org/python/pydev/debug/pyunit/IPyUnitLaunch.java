/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface IPyUnitLaunch {

    void stop();

    void relaunch();

    void relaunchTestResults(List<PyUnitTestResult> arrayList);

    void relaunchTestResults(List<PyUnitTestResult> arrayList, String mode);

    void fillXMLElement(Document document, Element launchElement);

}
