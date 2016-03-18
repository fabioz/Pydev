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
 * Abstract IInterpreterProvider that represents an already installed
 * InterpreterProvider.
 *
 * This is a convenience abstract class to simplify implementations of
 * IInterpreterProvider that are not installable.
 */
public abstract class AlreadyInstalledInterpreterProvider implements IInterpreterProvider {
    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void runInstall() {
        // Nothing to do
    }

    /**
     * Create a new array of already installed providers.
     *
     * Convenience method that creates an array that can be returned from
     * {@link IInterpreterProviderFactory#getInterpreterProviders(org.python.pydev.ui.pythonpathconf.IInterpreterProviderFactory.InterpreterType)}
     *
     * @param name
     *            Name of the Interpreter, see
     *            {@link IInterpreterProvider#getName()}
     * @param executableOrJar
     *            The Executable name, or Jar name, see
     *            {@link IInterpreterProvider#getExecutableOrJar()}
     * @return An array length 1 for the provided executableOrJar
     */
    static public IInterpreterProvider[] create(String name, String executableOrJar) {
        return create(name, new String[] { executableOrJar });
    }

    /**
     * Create a new array of already installed providers.
     *
     * Convenience method that creates an array that can be returned from
     * {@link IInterpreterProviderFactory#getInterpreterProviders(org.python.pydev.ui.pythonpathconf.IInterpreterProviderFactory.InterpreterType)}
     *
     * @param name
     *            Name of the Interpreter, see
     *            {@link IInterpreterProvider#getName()}
     * @param executableOrJars
     *            An array of the Executable name, or Jar name, see
     *            {@link IInterpreterProvider#getExecutableOrJar()}
     * @return An array the same length as executableOrJars
     */
    static public IInterpreterProvider[] create(final String name, final String[] executableOrJars) {
        IInterpreterProvider[] providers = new IInterpreterProvider[executableOrJars.length];
        for (int i = 0; i < executableOrJars.length; i++) {
            final String executableOrJar = executableOrJars[i];

            providers[i] = new AlreadyInstalledInterpreterProvider() {

                @Override
                public String getExecutableOrJar() {
                    return executableOrJar;
                }

                @Override
                public String getName() {
                    return name;
                }
            };
        }
        return providers;
    }
}
