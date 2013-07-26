package org.python.pydev.ui.pythonpathconf;

public class IronpythonInterpreterProviderFactory extends AbstractInterpreterProviderFactory {

    public IInterpreterProvider[] getInterpreterProviders(InterpreterType type) {
        if (type != IInterpreterProviderFactory.InterpreterType.IRONPYTHON) {
            return null;
        }

        return AlreadyInstalledInterpreterProvider.create("ipy", "ipy");
    }

}
