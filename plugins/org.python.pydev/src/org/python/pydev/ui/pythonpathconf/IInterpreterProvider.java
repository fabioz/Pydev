/******************************************************************************
* Copyright (C) 2013  Jonah Graham
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui.pythonpathconf;

/**
 * A representation of an interpreter that exists, or can be installed, on the
 * users machine. Created from calling all the registered
 * {@link IInterpreterProviderFactory#getInterpreterProviders(org.python.pydev.ui.pythonpathconf.IInterpreterProviderFactory.InterpreterType)}
 */
public interface IInterpreterProvider {

    /**
     * Return the name of the Executable or Jar
     *
     * @return the name of the Executable or Jar
     */
    public String getExecutableOrJar();

    /** 
     * Return user visible name. If the name is <code>null</code>, the
     * {@link #getExecutableOrJar()} will be used. If the name returned is not
     * unique within the workspace, will be made unique.
     *
     * @return the name displayed in the UI. May be <code>null</code>
     */
    public String getName();

    /**
     * Return whether the interpreter methods are returning provisional (false)
     * or installed (true) information. If not complete, call
     * {@link #runInstall()} to install/etc this interpreter.
     *
     * @return true if the Provider is ready to go
     */
    public boolean isInstalled();

    /**
     * Install the interpreter. If {@link #isInstalled()} returns false after
     * the install is run, then the user either cancelled, or an error occurred
     * that the user has already been informed about.
     *
     * It is up to the implementor to bundle installations up in a job to
     * maintain UI responsiveness.
     */
    public void runInstall();
}
