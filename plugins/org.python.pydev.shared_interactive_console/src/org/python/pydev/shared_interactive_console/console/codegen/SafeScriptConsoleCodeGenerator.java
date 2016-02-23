/******************************************************************************
* Copyright (C) 2012-2013  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
******************************************************************************/
package org.python.pydev.shared_interactive_console.console.codegen;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;

/**
 * Wrapper around an unknown IScriptConsoleCodeGenerator that catches and logs
 * exceptions using {@link SafeRunner}. 
 */
public class SafeScriptConsoleCodeGenerator implements IScriptConsoleCodeGenerator {
    private final IScriptConsoleCodeGenerator unsafeGenerator;

    private boolean hasPyCode;
    private String pyCode;

    private final class HasPyCodeRunnable implements ISafeRunnable {

        @Override
        public void run() throws Exception {
            hasPyCode = unsafeGenerator.hasPyCode();
        }

        @Override
        public void handleException(Throwable exception) {
            hasPyCode = false;
        }
    }

    private final class GetPyCodeRunnable implements ISafeRunnable {

        @Override
        public void run() throws Exception {
            pyCode = unsafeGenerator.getPyCode();
        }

        @Override
        public void handleException(Throwable exception) {
            pyCode = null;
        }
    }

    /**
     * Create a Safe wrapped generator for a possibly unsafe one.
     * @param unsafeGenerator generator to wrap
     */
    public SafeScriptConsoleCodeGenerator(IScriptConsoleCodeGenerator unsafeGenerator) {
        this.unsafeGenerator = unsafeGenerator;
    }

    /**
     * Calls nested generators getPyCode in a SafeRunner, on any exception
     * returns null
     */
    @Override
    public String getPyCode() {
        String ret;
        try {
            SafeRunner.run(new GetPyCodeRunnable());
            ret = pyCode;
        } finally {
            pyCode = null;
        }
        return ret;
    }

    /**
     * Calls nested generators getPyCode in a SafeRunner, on any exception
     * returns false
     */
    @Override
    public boolean hasPyCode() {
        SafeRunner.run(new HasPyCodeRunnable());
        return hasPyCode;
    }

}
