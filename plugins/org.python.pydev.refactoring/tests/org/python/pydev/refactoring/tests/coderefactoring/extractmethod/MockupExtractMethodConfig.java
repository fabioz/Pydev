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

package org.python.pydev.refactoring.tests.coderefactoring.extractmethod;

import java.util.HashMap;
import java.util.Map;

import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;

public class MockupExtractMethodConfig {

    private int offset;

    private int selectionLength;

    private int offsetStrategy;

    private Map<String, String> renameMap;

    public MockupExtractMethodConfig() {
        this.offset = 0;
        this.selectionLength = 0;
        this.offsetStrategy = IOffsetStrategy.AFTERINIT;
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

    public Map<String, String> getRenameMap() {
        if (renameMap == null) {
            this.renameMap = new HashMap<String, String>();
        }
        return renameMap;
    }

}
