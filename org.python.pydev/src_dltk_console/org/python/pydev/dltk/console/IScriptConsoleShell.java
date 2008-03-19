/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * This is the interface required by the console 'shell': meaning that those are the things
 * that must be asked to the server.
 */
public interface IScriptConsoleShell {

    /**
     * @param commandLine the current command in the buffer (still not entered)
     * @param position the relative position in the current buffer where the caret is 
     * @param offset the actual offset where the completions were requested (needed for creating the IProposals so that
     * they can be correctly applied).
     * 
     * @return the proposals to be applied.
     * @throws Exception
     */
    ICompletionProposal[] getCompletions(String commandLine, int position, int offset) throws Exception;

    /**
     * @param doc the document with all the contents of the console 
     * @param offset the offset that's being hovered in the viewer
     * 
     * @return the description to be shown to the user (hover)
     * @throws Exception
     */
    String getDescription(IDocument doc, int offset) throws Exception;

    /**
     * Closes the shell
     * 
     * @throws Exception
     */
    void close() throws Exception;
}
