/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;

import org.python.pydev.parser.jython.SimpleNode;

public class PredefinedSourceModule extends SourceModule {

    public PredefinedSourceModule(String name, File f, SimpleNode n, Throwable parseError) {
        super(name, f, n, parseError, null);
    }

}
