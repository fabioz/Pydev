/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.visitors;

public class MockupSelectionConfig {

    private int offset;

    private int selectionLength;

    private int offsetStrategy;

    public MockupSelectionConfig(int offset, int selectionLength, int offsetStrategy) {
        super();
        this.offset = offset;
        this.selectionLength = selectionLength;
        this.offsetStrategy = offsetStrategy;
    }

    public int getOffset() {
        return offset;
    }

    public int getOffsetStrategy() {
        return offsetStrategy;
    }

    public int getSelectionLength() {
        return selectionLength;
    }

}
