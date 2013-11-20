/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import org.python.pydev.core.FullRepIterable;

/**
 * @author fabioz
 *
 */
public final class ModInfo extends AbstractInfo {

    private static final long serialVersionUID = 1L;

    public ModInfo(String moduleDeclared) {
        super(getNameFromModule(moduleDeclared), moduleDeclared, null);
    }

    public ModInfo(String moduleDeclared, boolean doNotInternOnThisContstruct) {
        super(getNameFromModule(moduleDeclared), moduleDeclared, null, doNotInternOnThisContstruct);
    }

    private static String getNameFromModule(String moduleDeclared) {
        String lastPart = FullRepIterable.getLastPart(moduleDeclared);
        if (lastPart.equals("__init__")) {
            String withoutLastPart = FullRepIterable.getWithoutLastPart(moduleDeclared);
            return FullRepIterable.getLastPart(withoutLastPart);
        }
        return lastPart;
    }

    public int getType() {
        return MOD_IMPORT_TYPE;
    }

}
