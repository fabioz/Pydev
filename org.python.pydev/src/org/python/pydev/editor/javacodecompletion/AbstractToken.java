/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractToken implements IToken{

    private String rep;
    private String doc;

    public AbstractToken(String rep, String doc){
        this.rep = rep;
        this.doc = doc;
    }
    
    /**
     * @see org.python.pydev.editor.javacodecompletion.IToken#getRepresentation()
     */
    public String getRepresentation() {
        return rep;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.IToken#getDocStr()
     */
    public String getDocStr() {
        return doc;
    }

}
