package org.python.pydev.shared_core.partitioner;

import org.python.pydev.shared_core.string.FastStringBuffer;

public interface IContentsScanner {

    void getContents(int offset, int length, FastStringBuffer buffer);

}
