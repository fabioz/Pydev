/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 22, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.python.pydev.core.IPythonNature;

/**
 * A concrete token is created for representing modules.
 * 
 * E.g.: if there is a module called foo.bar.config, a concrete token for with rep: config 
 * and parentPackage foo.bar.config is created.
 * 
 * @author Fabio Zadrozny
 */
public final class ConcreteToken extends AbstractToken {

    private static final long serialVersionUID = 1L;

    /**
     * @param rep The representation for this token
     * @param doc the document that contains the token
     * @param parentPackage the parent package for the token (in this case, the module itself)
     * @param type the type for this token
     * @param iPythonNature 
     */
    public ConcreteToken(String rep, String doc, String args, String parentPackage, int type, IPythonNature nature) {
        super(rep, doc, args, parentPackage, type, nature);
    }

}
