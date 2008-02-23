/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;


/**
 * Class that makes the action of going to the next method
 */
public class PyNextMethod extends PyMethodNavigation{

    protected boolean getSearchForward(){
        return true;
    }

}
