/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.parser;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;

/**
 * The interface for an IParser
 * 
 * @author Fabio
 */
public interface IPyParser {

    /**
     * Notifies that a save has occurred (the parser may have to do a reparse when this happens)
     */
    void notifySaved();

    /**
     * Removes a listener from the parser
     */
    void removeParseListener(IParserObserver parserObserver);

    /**
     * Adds a listener from the parser
     */
    void addParseListener(IParserObserver parserObserver);

    /**
     * Disposes this parser (it may no longer be used after this method is called)
     */
    void dispose();

    /**
     * Sets the parameters on the heuristics for doing reparses
     */
    void resetTimeoutPreferences(boolean useAnalysisOnlyOnDocSave);

    /**
     * Sets the document and the input to be used.
     * 
     * @param document this is the document that is used in parses (the parser observes it for 
     * change notifications)
     * @param input this is the input from the editor. Used to get the file for creating markers if needed.
     */
    void setDocument(IDocument document, IEditorInput input);

    /**
     * Schedules a reparse in the parser.
     * @return 
     */
    boolean forceReparse(Object... argsToReparse);
}
