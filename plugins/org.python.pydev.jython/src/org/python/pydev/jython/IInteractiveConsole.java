/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 21, 2006
 */
package org.python.pydev.jython;

public interface IInteractiveConsole extends IPythonInterpreter {

    boolean push(String input);

}
