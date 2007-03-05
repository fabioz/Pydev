package org.python.pydev.refactoring.ast.adapters;

import java.util.List;

import org.python.pydev.parser.jython.ast.ClassDef;

public interface IClassDefAdapter extends INodeAdapter, IASTNodeAdapter<ClassDef>{

    public abstract List<String> getBaseClassNames();

    public abstract List<IClassDefAdapter> getBaseClasses();

    public abstract boolean hasBaseClass();

    public abstract List<SimpleAdapter> getAttributes();

    public abstract List<PropertyAdapter> getProperties();

    /**
     * @return a list of functions (disconsidering __init__ functions).
     */
    public abstract List<FunctionDefAdapter> getFunctionsInitFiltered();

    public abstract boolean hasFunctions();

    /**
     * @return true if there is any function disconsidering __init__ functions.
     */
    public abstract boolean hasFunctionsInitFiltered();

    public abstract boolean isNested();

    public abstract boolean hasAttributes();

    public abstract int getNodeBodyIndent();

    public abstract boolean hasInit();

    public abstract FunctionDefAdapter getFirstInit();

    public abstract List<SimpleAdapter> getAssignedVariables();

    public abstract boolean isNewStyleClass();

    public abstract List<FunctionDefAdapter> getFunctions();
}