/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

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
     * @return the path within the module to the name. E.g.: When we have:
     * class Test:
     *      def m1(self):
     *          pass
     *          
     * If this is the representation for the method m1, the path will be 'Test'
     */
    String getPath();
    
    /**
     * The type when it is a class with import
     */
    int CLASS_WITH_IMPORT_TYPE = 1;
    
    /**
     * The type when it is a method with import
     */
    int METHOD_WITH_IMPORT_TYPE = 2;
    
    /**
     * The type when it is an assign
     */
    int ATTRIBUTE_WITH_IMPORT_TYPE = 3;
    
    /**
     * The type when it is a name
     */
    int NAME_WITH_IMPORT_TYPE = 4;
    
    /**
     * @return the type of the information we are holding (given constants)
     */
    int getType();
}
