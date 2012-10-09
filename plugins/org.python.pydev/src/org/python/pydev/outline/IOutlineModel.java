/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Jul 25, 2003
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.StructuredSelection;
import org.python.pydev.parser.jython.SimpleNode;

/**
 * all the models in the outline view need to implement this interface
 */
public interface IOutlineModel {

    void dispose();

    /**
     * @return topmost object in the tree model
     * this object will be referenced in ContentProvider::getElements
     */
    ParsedItem getRoot();

    /**
     * this will be called in response to selection event
     * @param sel new selection
     * @return Point that contains line/column, or item to be selected
     */
    SimpleNode[] getSelectionPosition(StructuredSelection sel);
}
