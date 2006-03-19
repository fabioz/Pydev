/*
 * Created on Mar 19, 2006
 */
package org.python.pydev.core;

import java.io.Serializable;

/**
 * Defines a tuple of some object, adding equals and hashCode operations
 * 
 * @author Fabio
 */
public class Tuple3<X ,Y, Z> implements Serializable{

    public X o1;
    public Y o2;
    public Z o3;

    public Tuple3(X o1, Y o2, Z o3) {
        this.o1 = o1;
        this.o2 = o2;
        this.o3 = o3;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Tuple3)){
            return false;
        }
        
        Tuple3 t2 = (Tuple3) obj;
        if(!o1.equals(t2.o1)){
            return false;
        }
        if(!o2.equals(t2.o2)){
            return false;
        }
        if(!o3.equals(t2.o3)){
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        return o1.hashCode() * o2.hashCode() * o3.hashCode();
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
}
