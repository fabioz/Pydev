package org.python.pydev.parser.prettyprinterv2;

/**
 * Interface for the line part that marks an indent or dedent.
 */
public interface ILinePartIndentMark extends ILinePart{

    /**
     * @return true if a new line is required on the indent and false otherwise.
     * @note only applicable on indent.
     */
    public boolean getRequireNewLineOnIndent();
    
    public boolean isIndent();
}
