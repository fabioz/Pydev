package org.python.pydev.shared_core.partitioner;

/**
 * Defines the interface by which <code>WhitespaceRule</code>
 * determines whether a given character is to be considered
 * whitespace in the current context.
 */
public interface IWhitespaceDetector {

    /**
     * Returns whether the specified character is whitespace.
     *
     * @param c the character to be checked
     * @return <code>true</code> if the specified character is a whitespace char
     */
    boolean isWhitespace(char c);
}
