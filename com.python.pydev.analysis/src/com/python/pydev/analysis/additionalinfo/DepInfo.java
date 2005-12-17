/*
 * Created on 13/12/2005
 */
package com.python.pydev.analysis.additionalinfo;

public class DepInfo{
    public String moduleName;
    public String importsFrom;
    
    public DepInfo(String moduleName, String importsFrom){
        this.moduleName = moduleName;
        this.importsFrom = importsFrom;
    }
    
    @Override
    public int hashCode() {
        return this.moduleName.hashCode() + this.importsFrom.hashCode() * 7;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DepInfo)){
            return false;
        }
        DepInfo d = (DepInfo) obj;
        return this.moduleName.equals(d.moduleName) && this.importsFrom.equals(d.importsFrom);
    }

}