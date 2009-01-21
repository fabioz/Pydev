/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console;


public interface IScriptConsoleInterpreter extends IScriptConsoleShell, IConsoleRequest {

    /**
     * @param command the command (entered in the console) to be executed
     * @return the response from the interpreter.
     * @throws Exception if something wrong happened while doing the request.
     */
    InterpreterResponse exec(String command) throws Exception;


}
