package org.python.pydev.core.parser;


public interface IParserObserver3 {

    /**
     * Receives a structure (which can grow in attributes)
     */
    void parserChanged(ChangedParserInfoForObservers info);
    
    /**
     * Receives a structure (which can grow in attributes)
     */
    void parserError(ErrorParserInfoForObservers info);

}
