package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import org.eclipse.jdt.core.IJavaElement;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

public class JavaDefinition extends Definition{

    public IJavaElement javaElement;

    public JavaDefinition(String value, IModule module, IJavaElement javaElement) {
        super(-1, -1, value, null, null, module);
        this.javaElement = javaElement;
    }

}
