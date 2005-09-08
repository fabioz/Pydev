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

}
