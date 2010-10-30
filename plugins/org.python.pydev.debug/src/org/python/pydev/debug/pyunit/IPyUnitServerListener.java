package org.python.pydev.debug.pyunit;

public interface IPyUnitServerListener {

    void notifyTest(String status, String location, String test, String capturedOutput, String errorContents);

    void notifyDispose();

    void notifyFinished();

}
