package org.python.pydev.shared_interactive_console.console.ui.internal;

public interface IStreamMonitor {
    public void addListener(IStreamListener listener);

    public void removeListener(IStreamListener listener);
}
