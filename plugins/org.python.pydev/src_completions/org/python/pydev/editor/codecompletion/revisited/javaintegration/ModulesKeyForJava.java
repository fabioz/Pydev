package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.python.pydev.core.ModulesKey;

/**
 * This is the modules key that should be used for a given element found in a java project.
 *
 * @author Fabio
 */
public class ModulesKeyForJava extends ModulesKey{


    private IPackageFragmentRoot packageRoot;
    private IJavaElement javaElement;

    public ModulesKeyForJava(String elementName, IPackageFragmentRoot packageRoot, IJavaElement javaElement) {
        super(elementName, null);
        this.packageRoot = packageRoot;
        this.javaElement = javaElement;
    }

}
