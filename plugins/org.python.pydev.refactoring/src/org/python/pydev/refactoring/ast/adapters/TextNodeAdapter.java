/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

public class TextNodeAdapter implements INodeAdapter {

    private String name;

    public TextNodeAdapter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getParentName() {
        return "";
    }

    protected void setName(String name) {
        this.name = name;
    }

}
