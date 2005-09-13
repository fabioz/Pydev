/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;


public abstract class AbstractInfo implements IInfo{
    /**
     * the name
     */
    public String name;
    
    /**
     * the name of the module where this function is declared
     */
    public String moduleDeclared;


    public String getName() {
        return name;
    }

    public String getDeclaringModuleName() {
        return moduleDeclared;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof IInfo)){
            return false;
        }
        IInfo i = (IInfo) obj;
        
        
        if(i.getType() != getType()){
            return false;
        }

        if(!i.getDeclaringModuleName().equals(getDeclaringModuleName())){
            return false;
        }
        
        if(!i.getName().equals(getName())){
            return false;
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        return 7* getName().hashCode() + getDeclaringModuleName().hashCode() * getType();
    }
}
