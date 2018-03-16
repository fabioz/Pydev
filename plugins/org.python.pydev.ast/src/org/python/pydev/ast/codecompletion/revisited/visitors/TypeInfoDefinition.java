package org.python.pydev.ast.codecompletion.revisited.visitors;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.IModule;
import org.python.pydev.parser.visitors.TypeInfo;

public class TypeInfoDefinition extends Definition {

    public final TypeInfo info;

    public TypeInfoDefinition(Definition d, IModule module, TypeInfo info) {
        super(0, 0, info.getActTok(), null, null, module);
        Assert.isNotNull(info);
        this.info = info;
    }

}
