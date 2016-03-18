/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: fabioz
 * Created: February 2004
 */

package org.python.pydev.editor.actions;

/**
 * Class that makes the action of going to the previous method
 * 
 * @author Fabio Zadrozny
 */
public class PyPreviousMethod extends PyMethodNavigation {

    @Override
    protected boolean getSearchForward() {
        return false;
    }
}
