/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

public final class FuncInfo extends AbstractInfo {

    /**
     * Changed for 2.1
     */
    private static final long serialVersionUID = 3L;

    public FuncInfo(String defName, String moduleDeclared, String path) {
        super(defName, moduleDeclared, path);
    }

    public FuncInfo(String defName, String moduleDeclared, String path, boolean doNotInternOnThisContstruct) {
        super(defName, moduleDeclared, path, doNotInternOnThisContstruct);
    }

    public int getType() {
        return METHOD_WITH_IMPORT_TYPE;
    }

}