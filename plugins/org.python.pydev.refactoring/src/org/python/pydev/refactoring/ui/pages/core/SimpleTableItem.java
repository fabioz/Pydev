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

package org.python.pydev.refactoring.ui.pages.core;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class SimpleTableItem extends TableItem {

    private String realName;

    public SimpleTableItem(Table parent, String name) {
        super(parent, SWT.None);
        this.realName = name;
        setText(realName);
    }

    public SimpleTableItem(Table parent, String originalName, String text, int pos) {
        super(parent, SWT.None, pos);
        this.realName = originalName;
        setText(text);
    }

    public String getOriginalName() {
        return realName;
    }

    @Override
    protected void checkSubclass() {
    }

    public boolean hasNewName() {
        return this.realName.compareTo(getText()) != 0;
    }

}
