/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.interactive_console.console.ui.internal;

import org.eclipse.jface.text.IDocument;

import com.aptana.interactive_console.console.ui.IConsoleStyleProvider;

/**
 * Interface created just so that we can test the ScriptConsoleDocument listener (with the interfaces
 * it relies from the IScriptConsoleViewer2
 */
public interface IScriptConsoleViewer2ForDocumentListener {

    void setCaretOffset(int length, boolean async);

    IConsoleStyleProvider getStyleProvider();

    IDocument getDocument();

    void revealEndOfDocument();

}
