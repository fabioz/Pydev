/*
 * Created on Nov 22, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

/**
 * 
 * 
 * @author Fabio Zadrozny
 */
public class ConcreteToken extends AbstractToken{

    

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     * @param rep
     * @param doc
     * @param parentPackage
     * @param type
     */
    public ConcreteToken(String rep, String doc, String args, String parentPackage, int type) {
        super(rep, doc, args, parentPackage, type);
    }


}
