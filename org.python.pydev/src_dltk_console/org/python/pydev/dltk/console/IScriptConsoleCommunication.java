/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

public interface IScriptConsoleCommunication {
    
    InterpreterResponse execInterpreter(String command) throws Exception;

    public ICompletionProposal[] getCompletions(String text, int offset) throws Exception;
    
    public String getDescription(String text) throws Exception;

    void close() throws Exception;
    

}
