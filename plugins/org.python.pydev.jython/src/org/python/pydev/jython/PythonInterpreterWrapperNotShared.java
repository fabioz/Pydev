/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.jython;

import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class PythonInterpreterWrapperNotShared extends PythonInterpreter implements IPythonInterpreter {

    public PythonInterpreterWrapperNotShared() {
        super(null, createPySystemState());
    }

    public static PySystemState createPySystemState() {
        try {
            return new PySystemState();
        } catch (IllegalStateException e) {
            //happens when running tests.
            PySystemState.initialize();
            return new PySystemState();
        }
    }

}
