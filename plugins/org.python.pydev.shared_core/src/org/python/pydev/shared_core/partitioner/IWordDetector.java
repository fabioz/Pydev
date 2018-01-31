package org.python.pydev.shared_core.partitioner;

/**
 * Defines the interface by which <code>WordRule</code>
 * determines whether a given character is valid as part
 * of a word in the current context.
 */
public interface IWordDetector {

    /**
     * Returns whether the specified character is
     * valid as the first character in a word.
     *
     * @param c the character to be checked
     * @return <code>true</code> is a valid first character in a word, <code>false</code> otherwise
     */
    boolean isWordStart(char c);

    /**
     * Returns whether the specified character is
     * valid as a subsequent character in a word.
     *
     * @param c the character to be checked
     * @return <code>true</code> if the character is a valid word part, <code>false</code> otherwise
     */
    boolean isWordPart(char c);
}
