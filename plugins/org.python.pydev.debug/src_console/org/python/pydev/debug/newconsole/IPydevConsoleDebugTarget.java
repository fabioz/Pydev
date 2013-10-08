/******************************************************************************
* Copyright (C) 2012  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.newconsole;

/**
 * This is an interface used to connect a debug target that wants notifications
 * from the interactive console.
 */
public interface IPydevConsoleDebugTarget {

    /**
     * The interactive console (via {@link PydevConsoleCommunication} will call
     * setSuspended(true) when there is no user command currently running. When
     * a user command starts setSuspended(false) will be called.
     * 
     * Note that the console stays running (i.e. not suspended) even when
     * collecting input from the user via calls such as raw_input.
     * 
     * @param suspended
     *            Current suspended state
     */
    void setSuspended(boolean suspended);

}
