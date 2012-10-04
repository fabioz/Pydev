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
