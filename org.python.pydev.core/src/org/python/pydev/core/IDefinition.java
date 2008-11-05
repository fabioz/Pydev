/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

public interface IDefinition {

    IModule getModule();

    int getLine();

    int getCol();

    /**
     * @return the docstring for the definition.
     */
    String getDocstring();

}