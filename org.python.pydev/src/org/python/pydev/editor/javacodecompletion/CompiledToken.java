/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

/**
 * @author Fabio Zadrozny
 */
public class CompiledToken extends AbstractToken{

    /**
     * @param rep
     * @param doc
     */
    public CompiledToken(String rep, String doc) {
        super(rep, doc);
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.IToken#getCompletionType()
     */
    public int getCompletionType() {
        return -1;
    }

}
