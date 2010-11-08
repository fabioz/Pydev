package org.python.pydev.debug.pyunit;

import java.util.ArrayList;

public interface IPyUnitServer {

    void registerOnNotifyTest(IPyUnitServerListener pyUnitViewServerListener);

    void stop();

    void relaunch();

    void relaunchTestResults(ArrayList<PyUnitTestResult> arrayList);

}
