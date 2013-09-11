/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.analysis.additionalinfo;

public final class AttrInfo extends AbstractInfo {

    /**
     * Changed for 2.1
     */
    private static final long serialVersionUID = 3L;

    public AttrInfo(String name, String moduleDeclared, String path) {
        super(name, moduleDeclared, path);
    }

    public AttrInfo(String name, String moduleDeclared, String path, boolean doNotInternOnThisContstruct) {
        super(name, moduleDeclared, path, doNotInternOnThisContstruct);
    }

    public int getType() {
        return ATTRIBUTE_WITH_IMPORT_TYPE;
    }

}
