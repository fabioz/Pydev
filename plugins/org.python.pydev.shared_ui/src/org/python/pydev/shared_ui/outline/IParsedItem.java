/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.outline;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.shared_core.model.ErrorDescription;

public interface IParsedItem {

    IParsedItem[] getChildren();

    /**
     * @return the begin line of the node. Note: 1-based (and not 0-based).
     * -1 means unable to get.
     */
    int getBeginLine();

    /**
     * @return the begin column of the node. Note: 1-based (and not 0-based).
     * -1 means unable to get.
     */
    int getBeginCol();

    /**
     * If this item denotes an error, return the error description.
     */
    ErrorDescription getErrorDesc();

    Object getParent();

    Image getImage();

    void updateTo(IParsedItem newItem);

    boolean sameNodeType(IParsedItem newItem);

    void updateShallow(IParsedItem newItem);

    void setParent(IParsedItem parsedItem);

}
