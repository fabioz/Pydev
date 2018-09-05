package com.python.pydev.analysis.external;

import java.io.IOException;

public interface IExternalCodeAnalysisStream {

    void write(String string) throws IOException; // IOConsoleOutputStream

}
