/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.parsing;

import org.python.pydev.shared_core.string.FastStringBuffer;

public class ScopeEntry {

    public final int type;
    public final boolean open;
    public final int id;
    public final int offset;

    public ScopeEntry(int id, int type, boolean open, int offset) {
        this.type = type;
        this.open = open;
        this.id = id;
        this.offset = offset;
    }

    public void toString(FastStringBuffer temp) {
        if (open) {
            temp.append('[');
            temp.append(id);
            temp.append(' ');
        } else {
            temp.append(' ');
            temp.append(id);
            temp.append(']');
        }
    }

}
