/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import org.python.pydev.editor.codecompletion.revisited.AbstractToken;

/**
 * @author Fabio Zadrozny
 */
public class CompiledToken extends AbstractToken{

    /**
     * @param rep
     * @param doc
     */
    public CompiledToken(String rep, String doc, String parentPackage) {
        super(rep, doc, parentPackage, -1);
    }

    public CompiledToken(String rep, String doc, String parentPackage, int type){
        super(rep, doc, parentPackage, type);
    }

}
