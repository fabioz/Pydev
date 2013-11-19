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
package org.python.pydev.shared_core.partitioner;

public interface IMarkScanner {

    /**
     * @return a mark identifying the current offset.
     */
    public int getMark();

    /**
     * Resets the scanner to the current place.
     */
    public void setMark(int offset);

}
