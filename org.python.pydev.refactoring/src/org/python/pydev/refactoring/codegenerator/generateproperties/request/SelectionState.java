/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.generateproperties.request;

public class SelectionState {

    public final static int GETTER = 1;

    public final static int SETTER = 2;

    public final static int DELETE = 4;

    public final static int DOCSTRING = 8;

    private int flags = 0;

    public void addSelection(int type) {
        flags |= type;
    }

    public void removeSelection(int type) {
        flags &= ~type;
    }

    public boolean isGetter() {
        return (flags & GETTER) == GETTER;
    }

    public boolean isSetter() {
        return (flags & SETTER) == SETTER;
    }

    public boolean isDelete() {
        return (flags & DELETE) == DELETE;
    }

    public boolean isDocstring() {
        return (flags & DOCSTRING) == DOCSTRING;
    }

}
