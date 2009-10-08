package org.python.pydev.parser.prettyprinter;

import java.io.IOException;

import org.python.pydev.core.structure.FastStringBuffer;

public interface IWriterEraser {

    public void write(String o) throws IOException;
    
    public void erase(String o);

    public void pushTempBuffer();

    public String popTempBuffer();

    public boolean endsWithSpace();

    public FastStringBuffer getBuffer();
}
