package org.python.pydev.parser.prettyprinterv2;

/**
 * Line part that marks the start or end of a statement in the document.
 */
public interface ILinePartStatementMark {
    
    /**
     * @return true if we're starting the statement and false otherwise.
     */
    public boolean isStart();

}
