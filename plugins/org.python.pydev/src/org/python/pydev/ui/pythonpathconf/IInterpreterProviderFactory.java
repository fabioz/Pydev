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
