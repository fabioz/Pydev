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
    void resetTimeoutPreferences(boolean useAnalysisOnlyOnDocSave, int elapseMillisBeforeAnalysis);

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
     */
    void scheduleReparse();
}
