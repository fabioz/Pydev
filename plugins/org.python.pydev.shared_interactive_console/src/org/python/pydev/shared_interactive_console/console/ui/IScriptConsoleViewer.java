/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console.ui;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

/**
 * Interface that must be implemented by the console viewer. Provides info related to what
 * may be edited or not.
 */
public interface IScriptConsoleViewer extends ITextViewer {

    /**
     * @return the contents of the current buffer (text edited still not passed to the shell)
     */
    public String getCommandLine();

    /**
     * @return the offset where the current buffer starts (editable area of the document)
     */
    public int getCommandLineOffset();

    /**
     * @return the current caret offset.
     */
    public int getCaretOffset();

    /**
     * Sets the new caret offset.
     * 
     * @param offset the offset for the caret.
     */
    public void setCaretOffset(int offset, boolean async);

    /**
     * @return the document being viewed by this console viewer
     */
    @Override
    public IDocument getDocument();

    /**
     * @return the interpreter info (used to get the grammar version for resolving templates)
     */
    public Object getInterpreterInfo();

}
