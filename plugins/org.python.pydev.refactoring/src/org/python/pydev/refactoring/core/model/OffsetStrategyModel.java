/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.model;

import org.eclipse.jface.viewers.StructuredSelection;

public class OffsetStrategyModel extends StructuredSelection {

    private int key;

    private String description;

    public OffsetStrategyModel(int key, String description) {
        this.key = key;
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

    public int getStrategy() {
        return key;
    }

}
