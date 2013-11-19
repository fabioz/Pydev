/******************************************************************************
* Copyright (C) 2011-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.structure;

import java.util.Collection;
import java.util.LinkedHashSet;

public class OrderedSet<E> extends LinkedHashSet<E> {

    private static final long serialVersionUID = -9140695560309322962L;

    public OrderedSet() {

    }

    public OrderedSet(Collection<? extends E> c) {
        super(c);
    }

    public OrderedSet(int initialCapacity) {
        super(initialCapacity);
    }

}
