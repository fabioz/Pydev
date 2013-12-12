/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: atotic
 * Created: Jul 25, 2003
 */
package org.python.pydev.shared_core.parsing;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.model.ISimpleNode;

/**
 * PyParser broadcasts events to IParserListeners 
 * 
 * parserChanged is generated every time document is parsed successfully
 * parserError is generated when parsing fails
 */
public interface IParserObserver {

    /**
     * Every time document gets parsed, it generates a new parse tree.
     * 
     * Note that it may be called even if there are errors in the file (so, parserChanged will
     * be called and parserError will be called later on)
     * 
     * @param root the root of the new AST (abstract syntax tree)
     * @param file the file that has just been analyzed (it may be null)
     * 
     * It is meant to be an org.eclipse.core.resources.IFile or an 
     * org.eclipse.ui.internal.editors.text.JavaFileEditorInput (external files) or
     * PydevFileEditorInput.
     * 
     */
    void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc, long generatedOnStamp);

    /**
     * if parse generates an error, you'll get this event
     * the exception class will be ParseException, or TokenMgrError
     * @param file the file that has just been analyzed (it may be null)
     */
    void parserError(Throwable error, IAdaptable file, IDocument doc);
}
