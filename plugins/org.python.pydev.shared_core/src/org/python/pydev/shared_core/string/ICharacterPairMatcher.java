package org.python.pydev.shared_core.string;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * A character pair matcher finds to a character at a certain document offset the matching peer
 * character. It is the matchers responsibility to define the concepts of "matching" and "peer". The
 * matching process starts at a given offset. Starting of this offset, the matcher chooses a
 * character close to this offset. The anchor defines whether the chosen character is left or right
 * of the initial offset. The matcher then searches for the matching peer character of the chosen
 * character and if it finds one, delivers the minimal region of the document that contains both
 * characters.
 *
 * <p>
 * In order to provide backward compatibility for clients of <code>ICharacterPairMatcher</code>,
 * extension interfaces are used to provide a means of evolution. The following extension interface
 * exists:
 * <ul>
 * <li>{@link org.eclipse.jface.text.source.ICharacterPairMatcherExtension} since version 3.8
 * introducing the concept of matching peer character and enclosing peer characters for a given
 * selection.</li>
 * </ul>
 * </p>
 * <p>
 * Clients may implement this interface and its extension interface or use the default
 * implementation provided by <code>DefaultCharacterPairMatcher</code>.
 * </p>
 *
 * @see org.eclipse.jface.text.source.ICharacterPairMatcherExtension
 * @since 2.1
 */
public interface ICharacterPairMatcher {

    /**
     * Indicates the anchor value "right".
     */
    int RIGHT = 0;
    /**
     * Indicates the anchor value "left".
     */
    int LEFT = 1;

    /**
     * Disposes this pair matcher.
     */
    void dispose();

    /**
     * Clears this pair matcher. I.e. the matcher throws away all state it might
     * remember and prepares itself for a new call of the <code>match</code>
     * method.
     */
    void clear();

    /**
     * Starting at the given offset, the matcher chooses a character close to this offset. The
     * matcher then searches for the matching peer character of the chosen character and if it finds
     * one, returns the minimal region of the document that contains both characters.
     *
     * <p>
     * Since version 3.8 the recommended way for finding matching peers is to use
     * {@link org.eclipse.jface.text.source.ICharacterPairMatcherExtension#match(IDocument, int, int)}
     * .
     * </p>
     *
     * @param document the document to work on
     * @param offset the start offset
     * @return the minimal region containing the peer characters or <code>null</code> if there is no
     *         peer character.
     */
    IRegion match(IDocument document, int offset);

    /**
     * Returns the anchor for the region of the matching peer characters. The anchor says whether
     * the character that has been chosen to search for its peer character has been the left or the
     * right character of the pair.
     *
     * @return <code>RIGHT</code> or <code>LEFT</code>
     */
    int getAnchor();
}
