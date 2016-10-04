/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IPythonNature;

/**
 * @author fabioz
 *
 */
public final class ModInfo extends AbstractInfo {

    private static final long serialVersionUID = 1L;

    public ModInfo(String moduleDeclared, IPythonNature nature) {
        super(getNameFromModule(moduleDeclared), moduleDeclared, null, nature);
    }

    public ModInfo(String moduleDeclared, boolean doNotInternOnThisContstruct, IPythonNature nature) {
        super(getNameFromModule(moduleDeclared), moduleDeclared, null, doNotInternOnThisContstruct, nature);
    }

    private static String getNameFromModule(String moduleDeclared) {
        String lastPart = FullRepIterable.getLastPart(moduleDeclared);
        if (lastPart.equals("__init__")) {
            String withoutLastPart = FullRepIterable.getWithoutLastPart(moduleDeclared);
            return FullRepIterable.getLastPart(withoutLastPart);
        }
        return lastPart;
    }

    @Override
    public int getType() {
        return MOD_IMPORT_TYPE;
    }

}
