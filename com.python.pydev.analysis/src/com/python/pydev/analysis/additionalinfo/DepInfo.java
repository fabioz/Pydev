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
    
    /**
     * This constructor should be used if it is a dependency generated from a wild import
     * 
     * @param moduleName this is the name of the module that is dependent on the token
     */
    public DepInfo(String moduleName){
        this.moduleName = moduleName;
        this.importsFrom = null;
    }
    
    public boolean isFromWildImport() {
        return this.importsFrom == null;
    }
    
    @Override
    public int hashCode() {
        int modNameHashCode = this.moduleName.hashCode();
        int importHashCode = this.importsFrom != null ? this.importsFrom.hashCode() : 9;
        return modNameHashCode + importHashCode * 7;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DepInfo)){
            return false;
        }
        DepInfo d = (DepInfo) obj;
        return this.moduleName.equals(d.moduleName) && this.importsFrom.equals(d.importsFrom);
    }

    @Override
    public String toString() {
        return "<DepInfo ["+moduleName+" depends on "+this.importsFrom+"]>";
    }
}