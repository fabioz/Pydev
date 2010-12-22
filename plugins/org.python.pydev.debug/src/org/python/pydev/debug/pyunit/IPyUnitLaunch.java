package org.python.pydev.debug.pyunit;

import java.util.List;

public interface IPyUnitLaunch {

    void stop();

    void relaunch();

    void relaunchTestResults(List<PyUnitTestResult> arrayList);
    
    void relaunchTestResults(List<PyUnitTestResult> arrayList, String mode);

}
