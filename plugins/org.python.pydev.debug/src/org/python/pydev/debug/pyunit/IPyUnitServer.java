package org.python.pydev.debug.pyunit;

public interface IPyUnitServer {

    void registerOnNotifyTest(IPyUnitServerListener pyUnitViewServerListener);

    void stop();

    void relaunch();

}
