/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/08/2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.listing_utils;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.shared_core.callbacks.ICallback0;

/**
 * copied from org.eclipse.jdt.internal.launching.StandardVMType
 *
 * @author Fabio Zadrozny
 */
public class JavaVmLocationFinder {

    private JavaVmLocationFinder() {
        super();
    }

    /**
     * @return the default java executable configured in the jdt plugin
     */
    public static File findDefaultJavaExecutable() throws JDTNotAvailableException {
        Assert.isNotNull(callbackJavaExecutable != null,
                "JavaVmLocationFinder.callbackJavaExecutable must be set prior to use.");
        return callbackJavaExecutable.call();
    }

    /**
     * @return the default java jars (rt.jar ... )
     */
    public static List<File> findDefaultJavaJars() throws JDTNotAvailableException {
        Assert.isNotNull(callbackJavaJars != null,
                "JavaVmLocationFinder.callbackJavaJars must be set prior to use.");
        return callbackJavaJars.call();
    }

    /**
     * Might be changed for tests (if not in the eclipse env)
     */
    public static ICallback0<File> callbackJavaExecutable;

    /**
     * Might be changed for tests (if not in the eclipse env)
     */
    public static ICallback0<List<File>> callbackJavaJars;
}
