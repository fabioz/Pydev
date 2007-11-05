package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.python.pydev.core.ModulesKey;

/**
 * This is the modules key that should be used for a given element found in a java project.
 *
 * @author Fabio
 */
public class ModulesKeyForJava extends ModulesKey{

    private IPackageFragment packageFragment;
    private IType type;

    public ModulesKeyForJava(String name, IPackageFragment packageFragment) {
        super(name, null);
        this.packageFragment = packageFragment;
    }

    public ModulesKeyForJava(String name, IType type) {
        super(name, null);
        this.type = type;
    }

}
