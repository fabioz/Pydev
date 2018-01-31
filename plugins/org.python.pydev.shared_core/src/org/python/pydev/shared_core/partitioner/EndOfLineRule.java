package org.python.pydev.shared_core.partitioner;

/**
 * A specific configuration of a single line rule
 * whereby the pattern begins with a specific sequence but
 * is only ended by a line delimiter.
 */
public class EndOfLineRule extends SingleLineRule {

    /**
     * Creates a rule for the given starting sequence
     * which, if detected, will return the specified token.
     *
     * @param startSequence the pattern's start sequence
     * @param token the token to be returned on success
     */
    public EndOfLineRule(String startSequence, IToken token) {
        this(startSequence, token, (char) 0);
    }

    /**
     * Creates a rule for the given starting sequence
     * which, if detected, will return the specified token.
     * Any character which follows the given escape character
     * will be ignored.
     *
     * @param startSequence the pattern's start sequence
     * @param token the token to be returned on success
     * @param escapeCharacter the escape character
     */
    public EndOfLineRule(String startSequence, IToken token, char escapeCharacter) {
        super(startSequence, null, token, escapeCharacter, true);
    }

    /**
     * Creates a rule for the given starting sequence
     * which, if detected, will return the specified token.
     * Any character which follows the given escape character
     * will be ignored. In addition, an escape character
     * immediately before an end of line can be set to continue
     * the line.
     *
     * @param startSequence the pattern's start sequence
     * @param token the token to be returned on success
     * @param escapeCharacter the escape character
     * @param escapeContinuesLine indicates whether the specified escape
     *        character is used for line continuation, so that an end of
     *        line immediately after the escape character does not
     *        terminate the line, even if <code>breakOnEOL</code> is true
     * @since 3.0
     */
    public EndOfLineRule(String startSequence, IToken token, char escapeCharacter, boolean escapeContinuesLine) {
        super(startSequence, null, token, escapeCharacter, true, escapeContinuesLine);
    }
}
