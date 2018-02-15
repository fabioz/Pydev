package org.python.pydev.shared_core.partitioner;

/**
 * A token to be returned by a rule.
 */
public interface IToken {

    /**
     * Return whether this token is undefined.
     *
     * @return <code>true</code>if this token is undefined
     */
    boolean isUndefined();

    /**
     * Return whether this token represents a whitespace.
     *
     * @return <code>true</code>if this token represents a whitespace
     */
    boolean isWhitespace();

    /**
     * Return whether this token represents End Of File.
     *
     * @return <code>true</code>if this token represents EOF
     */
    boolean isEOF();

    /**
     * Return whether this token is neither undefined, nor whitespace, nor EOF.
     *
     * @return <code>true</code>if this token is not undefined, not a whitespace, and not EOF
     */
    boolean isOther();

    /**
     * Return a data attached to this token. The semantics of this data kept undefined by this interface.
     *
     * @return the data attached to this token.
     */
    Object getData();
}
