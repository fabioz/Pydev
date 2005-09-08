/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

public interface IInfo {

    /**
     * @return the name of the representing token
     */
    String getName();

    /**
     * @return the name of the module that declares this information
     */
    String getDeclaringModuleName();
}
