/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/06/2005
 */
package org.python.pydev.core;

import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.core.parser.IParserObserver;

/**
 * @author Fabio
 */
public interface IPyEdit extends IParserObserver {

    /**
     * @return the python nature used in this editor
     * @throws NotConfiguredInterpreterException 
     * @throws MisconfigurationException 
     */
    IPythonNature getPythonNature() throws MisconfigurationException;

    /**
     * @return the editor input
     */
    IEditorInput getEditorInput();

    /**
     * This map may be used by clients to store info regarding this editor.
     * 
     * Clients should be careful so that this key is unique and does not conflict with other
     * plugins. 
     * 
     * This is not enforced.
     * 
     * The suggestion is that the cache key is always preceded by the class name that will use it.
     */
    Map<String, Object> getCache();

    /**
     * @return whether this edit and the one passed as a parameter have the same input.
     */
    boolean hasSameInput(IPyEdit edit);

    IDocument getDocument();

    /**
     * Set status message
     */
    void setStatusLineErrorMessage(String msg);

    IGrammarVersionProvider getGrammarVersionProvider();

    IIndentPrefs getIndentPrefs();
}
