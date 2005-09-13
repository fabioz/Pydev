/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.io.Serializable;

public interface IInfo extends Serializable {

    /**
     * @return the name of the representing token
     */
    String getName();

    /**
     * @return the name of the module that declares this information
     */
    String getDeclaringModuleName();
    
    /**
     * The type when it is a class with import
     */
    int CLASS_WITH_IMPORT_TYPE = 1;
    
    /**
     * The type when it is a method with import
     */
    int METHOD_WITH_IMPORT_TYPE = 2;
    
    /**
     * @return the type of the information we are holding (given constants)
     */
    int getType();
}
