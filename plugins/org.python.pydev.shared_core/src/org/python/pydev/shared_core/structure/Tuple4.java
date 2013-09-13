/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 19, 2006
 */
package org.python.pydev.shared_core.structure;

import java.io.Serializable;

import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Defines a tuple of some object, adding equals and hashCode operations
 * 
 * @author Fabio
 */
public final class Tuple4<X, Y, Z, T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public X o1;
    public Y o2;
    public Z o3;
    public T o4;

    public Tuple4(X o1, Y o2, Z o3, T o4) {
        this.o1 = o1;
        this.o2 = o2;
        this.o3 = o3;
        this.o4 = o4;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Tuple4)) {
            return false;
        }

        Tuple4 t2 = (Tuple4) obj;
        if (!o1.equals(t2.o1)) {
            return false;
        }
        if (!o2.equals(t2.o2)) {
            return false;
        }
        if (!o3.equals(t2.o3)) {
            return false;
        }
        if (!o4.equals(t2.o4)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return o1.hashCode() * o2.hashCode() * o3.hashCode() * o4.hashCode();
    }

    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("Tuple [");
        buffer.appendObject(o1);
        buffer.append(" -- ");
        buffer.appendObject(o2);
        buffer.append(" -- ");
        buffer.appendObject(o3);
        buffer.append(" -- ");
        buffer.appendObject(o4);
        buffer.append("]");
        return buffer.toString();
    }
}
