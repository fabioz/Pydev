package org.python.pydev.debug.pyunit;

import java.util.ArrayList;

public interface IPyUnitLaunch {

    void stop();

    void relaunch();

    void relaunchTestResults(ArrayList<PyUnitTestResult> arrayList);

}
