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
 * A factory for creating {@link IInterpreterProvider}s
 *
 * Instances of this class should be registered with the
 * org.python.pydev.pydev_interpreter_provider extension point.
 */
public interface IInterpreterProviderFactory {

	/**
	 * Possible types of Interpreters that a factory can create providers for.
	 */
	public enum InterpreterType {
		PYTHON, JYTHON, IRONPYTHON
	}

	/**
	 * Create a (possibly empty or <code>null</code>) array of
	 * {@link IInterpreterProvider}s that contain information about current, or
	 * potential, Python installations of type <code>type</code>
	 *
	 * @param type
	 *            the type of Interpreter to create providers for
	 * @return Array of possible providers. Can be empty or null if no providers
	 *         for given type can be created.
	 */
	public IInterpreterProvider[] getInterpreterProviders(InterpreterType type);
}
