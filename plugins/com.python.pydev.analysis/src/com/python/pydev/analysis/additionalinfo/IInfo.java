/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import org.python.pydev.core.IPythonNature;

public interface IInfo extends Comparable<IInfo> {

    /**
     * @return the name of the representing token
     * 
     * Cannot be null.
     */
    String getName();

    /**
     * @return the name of the module that declares this information
     * 
     * Cannot be null.
     */
    String getDeclaringModuleName();

    /**
     * @return the path within the module to the name. E.g.: When we have:
     * class Test:
     *      def m1(self):
     *          pass
     *          
     * If this is the representation for the method m1, the path will be 'Test'
     * 
     * This field may be null!
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
     * The type when it is a module
     */
    int MOD_IMPORT_TYPE = 5;

    /**
     * @return the type of the information we are holding (given constants)
     */
    int getType();

    IPythonNature getNature();
}
