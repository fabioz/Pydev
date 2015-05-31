/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * This is the modules key that should be used for a given element found in a java project.
 *
 * @author Fabio
 */
public class ModulesKeyForJava extends ModulesKey {

    private IPackageFragmentRoot packageRoot;
    private IJavaElement javaElement;

    public ModulesKeyForJava(String elementName, IPackageFragmentRoot packageRoot, IJavaElement javaElement) {
        super(elementName, null);
        this.packageRoot = packageRoot;
        this.javaElement = javaElement;
    }

    @Override
    public void toIO(FastStringBuffer buf) {
        throw new RuntimeException("ModulesKeyForJava cannot go to IO (it depends on in-memory information from JDT).");
    }

}
