/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

/**
 * Class that makes the action of going to the previous method
 * 
 * @author Fabio Zadrozny
 */
public class PyPreviousMethod extends PyMethodNavigation {

    protected boolean getSearchForward(){
        return false;
    }
}
