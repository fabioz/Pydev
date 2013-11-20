package org.python.pydev.core;

public interface IInterpreterManagerListener {

    /**
     * Called after infos are set (changed) in the interpreter manager.
     */
    void afterSetInfos(IInterpreterManager manager, IInterpreterInfo[] interpreterInfos);

}
