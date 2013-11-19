/******************************************************************************
* Copyright (C) 2007-2009  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.utils;

import java.util.ArrayList;
import java.util.List;

public final class ListUtils {

    private ListUtils() {
    }

    public static <E> List<E> wrap(E element) {
        List<E> list = new ArrayList<E>();
        list.add(element);
        return list;
    }

}
