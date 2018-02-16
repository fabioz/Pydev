package com.python.pydev.analysis.pylint;

import java.io.IOException;

public interface IPyLintStream {

    void write(String string) throws IOException; // IOConsoleOutputStream

}
