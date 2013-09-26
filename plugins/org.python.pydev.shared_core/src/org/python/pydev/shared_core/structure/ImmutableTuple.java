/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package org.python.pydev.shared_core.structure;

import java.io.Serializable;

import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Defines a tuple of some object, adding equals and hashCode operations
 * 
 * All attributes in this class are final!
 * 
 * @author Fabio
 */
public final class ImmutableTuple<X, Y> implements Serializable {

    private static final long serialVersionUID = 1L;

    public final X o1;
    public final Y o2;

    public ImmutableTuple(X o1, Y o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImmutableTuple)) {
            return false;
        }

        ImmutableTuple t2 = (ImmutableTuple) obj;
        if (o1 == t2.o1 && o2 == t2.o2) { //all the same 
            return true;
        }

        if (o1 == null && t2.o1 != null) {
            return false;
        }
        if (o2 == null && t2.o2 != null) {
            return false;
        }
        if (o1 != null && t2.o1 == null) {
            return false;
        }
        if (o2 != null && t2.o2 == null) {
            return false;
        }

        if (!o1.equals(t2.o1)) {
            return false;
        }
        if (!o2.equals(t2.o2)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (o1 != null && o2 != null) {
            return o1.hashCode() * o2.hashCode();
        }
        if (o1 != null) {
            return o1.hashCode();
        }
        if (o2 != null) {
            return o2.hashCode();
        }
        return 7;
    }

    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("Tuple [");
        buffer.appendObject(o1);
        buffer.append(" -- ");
        buffer.appendObject(o2);
        buffer.append("]");
        return buffer.toString();
    }
}
