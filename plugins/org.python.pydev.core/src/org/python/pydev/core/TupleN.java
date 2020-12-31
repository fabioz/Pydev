/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package org.python.pydev.core;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Defines a tuple of some object, adding equals and hashCode operations
 *
 * @author Fabio
 */
public final class TupleN implements Serializable {

    private static final long serialVersionUID = 1L;

    public Object[] o1;

    public TupleN(Object... o1) {
        this.o1 = o1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TupleN other = (TupleN) obj;
        if (!Arrays.deepEquals(o1, other.o1)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.deepHashCode(o1);
        return result;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Tuple [");
        for (int i = 0; i < o1.length; i++) {
            buffer.append(o1[i]);
            if (i != o1.length - 1) {
                buffer.append(", ");
            }
        }
        buffer.append("]");
        return buffer.toString();
    }
}
