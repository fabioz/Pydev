/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.AbstractToken;

/**
 * @author Fabio Zadrozny
 */
public class CompiledToken extends AbstractToken {

    private static final long serialVersionUID = 1L;

    public CompiledToken(String rep, String doc, String args, String parentPackage, int type, IPythonNature nature) {
        super(rep, doc, args, parentPackage, type, nature);
    }

}
