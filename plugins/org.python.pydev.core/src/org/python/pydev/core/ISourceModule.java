package org.python.pydev.core;

public interface ISourceModule extends IModule{

    /**
     * @return a Module (it is declared as object because in the core we do not have access to the SimpleNode or Module)
     */
    public Object getAst();
}
