/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial API and implementation
*     Andrew Ferrazzutti <aferrazz@redhat.com> - ongoing maintenance
******************************************************************************/
package junit3.runner;

import java.util.Enumeration;

/**
 * Collects Test class names to be presented
 * by the TestSelector. 
 * @see TestSelector
 */
public interface TestCollector {
    /**
     * Returns an enumeration of Strings with qualified class names
     */
    public Enumeration<String> collectTests();
}
