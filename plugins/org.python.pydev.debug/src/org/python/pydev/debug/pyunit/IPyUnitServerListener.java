package org.python.pydev.debug.pyunit;

public interface IPyUnitServerListener {
    
    void notifyTestsCollected(String totalTestsCount);

    void notifyTest(String status, String location, String test, String capturedOutput, String errorContents, String time);

    void notifyDispose();

    void notifyFinished(String totalTimeInSecs);

    void notifyStartTest(String location, String test);


}
