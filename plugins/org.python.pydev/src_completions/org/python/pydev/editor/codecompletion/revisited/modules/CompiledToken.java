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

    private static final long serialVersionUID = 1L;

    public CompiledToken(String rep, String doc, String args, String parentPackage, int type){
        super(rep, doc, args, parentPackage, type);
    }
    
}
