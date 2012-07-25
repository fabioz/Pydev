/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.dltk.console.ui;

import java.util.List;

import org.python.pydev.core.Tuple;

/**
 * Interface that will create the style range for the contents entered in the console.
 */
public interface IConsoleStyleProvider {

    ScriptStyleRange createPromptStyle(String prompt, int offset);

    ScriptStyleRange createUserInputStyle(String content, int offset);

    Tuple<List<ScriptStyleRange>, String> createInterpreterOutputStyle(String content, int offset);

    Tuple<List<ScriptStyleRange>, String> createInterpreterErrorStyle(String content, int offset);
}
