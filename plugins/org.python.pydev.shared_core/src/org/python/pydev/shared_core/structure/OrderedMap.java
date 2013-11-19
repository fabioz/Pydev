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

import java.util.LinkedHashMap;

public class OrderedMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 3463361572350039096L;

    public OrderedMap() {
        super();
    }

    public OrderedMap(int initialCapacity) {
        super(initialCapacity);
    }

}
