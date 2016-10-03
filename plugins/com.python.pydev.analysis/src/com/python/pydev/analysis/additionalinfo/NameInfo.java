/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.analysis.additionalinfo;

import org.python.pydev.core.IPythonNature;

public final class NameInfo extends AbstractInfo {

    /**
     * Changed for 2.1
     */
    private static final long serialVersionUID = 3L;

    public NameInfo(String name, String moduleDeclared, String path, IPythonNature nature) {
        super(name, moduleDeclared, path, nature);
    }

    public NameInfo(String name, String moduleDeclared, String path, boolean doNotInternOnThisContstruct,
            IPythonNature nature) {
        super(name, moduleDeclared, path, doNotInternOnThisContstruct, nature);
    }

    @Override
    public int getType() {
        return NAME_WITH_IMPORT_TYPE;
    }

}
