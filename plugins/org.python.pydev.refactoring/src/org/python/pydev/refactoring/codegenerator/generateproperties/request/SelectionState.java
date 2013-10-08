/******************************************************************************
* Copyright (C) 2006-2009  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.codegenerator.generateproperties.request;

public class SelectionState {
    public static final int GETTER = 1;
    public static final int SETTER = 2;
    public static final int DELETE = 4;
    public static final int DOCSTRING = 8;

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
