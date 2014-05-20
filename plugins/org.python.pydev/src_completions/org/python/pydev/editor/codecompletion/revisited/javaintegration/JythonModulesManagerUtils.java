/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModuleForZip;

/**
 * Wrap things related to jython (specially dependent on JDT)
 * 
 * So, all accesses that require creation of objects dependent on JDT should be done through this class.
 * 
 * @author Fabio
 */
public class JythonModulesManagerUtils {

    public static AbstractModule createModuleFromJar(EmptyModuleForZip emptyModuleForZip)
            throws JDTNotAvailableException {
        try {
            return new JavaZipModule(emptyModuleForZip);
        } catch (Throwable e) {
            Log.log("Unable to create java module for (note: JDT is required for Jython development): "
                    + emptyModuleForZip);
            tryRethrowAsJDTNotAvailableException(e);
            throw new RuntimeException("Should never get here", e);
        }
    }

    /**
     * Handles the exception and re-throws it as a JDTNotAvailableException (if it was a LinkageError or a 
     * ClassNotFoundException or a JDTNotAvailableException) or creates a RuntimeException and throws this exception
     * encapsulating the previous one
     * 
     * @param e the exception that should be transformed to a JDTNotAvailableException (if possible)
     * @throws JDTNotAvailableException
     */
    public static void tryRethrowAsJDTNotAvailableException(Throwable e) throws JDTNotAvailableException {
        if (isOptionalJDTClassNotFound(e)) {
            throw new JDTNotAvailableException();

        } else if (e instanceof JDTNotAvailableException) {
            JDTNotAvailableException jdtNotAvailableException = (JDTNotAvailableException) e;
            throw jdtNotAvailableException;

        } else if (e instanceof RuntimeException) {
            RuntimeException runtimeException = (RuntimeException) e;
            throw runtimeException;
        }

        Log.log(e);
        throw new RuntimeException(e);
    }

    /**
     * @return true if the passed throwable belongs to a class of exceptions related to not having JDT available
     */
    public static boolean isOptionalJDTClassNotFound(Throwable e) {
        return e instanceof LinkageError || e instanceof ClassNotFoundException || e instanceof NoClassDefFoundError;
    }

}
