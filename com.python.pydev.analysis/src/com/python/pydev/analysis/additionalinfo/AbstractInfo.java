/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;


public abstract class AbstractInfo implements IInfo{
    /**
     * the name
     */
    public String name;
    
    /**
     * This is the path (may be null)
     */
    public String path;
    
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
    
    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof IInfo)){
            return false;
        }
        IInfo otherInfo = (IInfo) obj;
        
        
        if(otherInfo.getType() != getType()){
            return false;
        }

        if(!otherInfo.getDeclaringModuleName().equals(getDeclaringModuleName())){
            return false;
        }
        
        if(!otherInfo.getName().equals(getName())){
            return false;
        }
        
        //if one of them is null, the other must also be null...
        if((otherInfo.getPath() == null || getPath() == null)){
            if(otherInfo.getPath() != getPath()){
                //one of them is not null
                return false;
            }
            //both are null
            return true;
        }
        
        //they're not null
        if(!otherInfo.getPath().equals(getPath())){
            return false;
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        return 7* getName().hashCode() + getDeclaringModuleName().hashCode() * getType();
    }
    
    @Override
    public String toString() {
        return getName()+ " ("+getDeclaringModuleName()+") - Path:"+getPath();
    }
}
