/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.io.File;

/**
 * @author fabioz
 */
public interface IExceptionsBreakpointListener {

    /**
     * Called when the list of exceptions is changed or when the option on whether to handle
     * caught or uncaught exceptions is changed.
     */
    void onSetConfiguredExceptions();

    void onAddIgnoreThrownExceptionIn(File file, int lineNumber);

    void onUpdateIgnoreThrownExceptions();

}
