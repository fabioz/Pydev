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

/**
 * Defines a tuple of some object, adding equals and hashCode operations
 * 
 * @author Fabio
 */
public final class Tuple3<X, Y, Z> implements Serializable {

    private static final long serialVersionUID = 1L;

    public X o1;
    public Y o2;
    public Z o3;

    public Tuple3(X o1, Y o2, Z o3) {
        this.o1 = o1;
        this.o2 = o2;
        this.o3 = o3;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Tuple [");
        buffer.append(o1);
        buffer.append(" -- ");
        buffer.append(o2);
        buffer.append(" -- ");
        buffer.append(o3);
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Auto-generated code to deal with nulls.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((o1 == null) ? 0 : o1.hashCode());
        result = prime * result + ((o2 == null) ? 0 : o2.hashCode());
        result = prime * result + ((o3 == null) ? 0 : o3.hashCode());
        return result;
    }

    /**
     * Auto-generated code to deal with nulls.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Tuple3)) {
            return false;
        }
        final Tuple3 other = (Tuple3) obj;
        if (o1 == null) {
            if (other.o1 != null)
                return false;
        } else if (!o1.equals(other.o1))
            return false;
        if (o2 == null) {
            if (other.o2 != null)
                return false;
        } else if (!o2.equals(other.o2))
            return false;
        if (o3 == null) {
            if (other.o3 != null)
                return false;
        } else if (!o3.equals(other.o3))
            return false;
        return true;
    }
}
