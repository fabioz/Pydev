/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import org.eclipse.jdt.core.IJavaElement;
import org.python.pydev.ast.codecompletion.revisited.modules.IJavaDefinition;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.core.IModule;

public class JavaDefinition extends Definition implements IJavaDefinition {

    public IJavaElement javaElement;

    public JavaDefinition(String value, IModule module, IJavaElement javaElement) {
        super(-1, -1, value, null, null, module);
        this.javaElement = javaElement;
    }

}
