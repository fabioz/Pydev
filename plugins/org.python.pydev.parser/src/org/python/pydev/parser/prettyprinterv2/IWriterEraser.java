/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

import java.io.IOException;

import org.python.pydev.shared_core.string.FastStringBuffer;

public interface IWriterEraser {

    public void write(String o) throws IOException;

    public void erase(String o);

    public void pushTempBuffer();

    public String popTempBuffer();

    public boolean endsWithSpace();

    public FastStringBuffer getBuffer();
}
