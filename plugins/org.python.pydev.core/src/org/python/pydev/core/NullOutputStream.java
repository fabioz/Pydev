/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Fabio
 *
 */
public class NullOutputStream extends OutputStream {

    public static final NullOutputStream singleton = new NullOutputStream();

    private NullOutputStream() {

    }

    @Override
    public void write(int b) throws IOException {
    }

}
