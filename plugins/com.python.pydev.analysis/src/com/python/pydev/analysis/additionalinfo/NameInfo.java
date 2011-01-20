/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.analysis.additionalinfo;

import org.python.pydev.core.ObjectsPool;

public class NameInfo extends AbstractInfo{

    private static final long serialVersionUID = 1L;

    public static NameInfo fromName(String name, String moduleDeclared, String path) {
        NameInfo info = new NameInfo();
        info.name = ObjectsPool.intern(name);
        info.moduleDeclared = moduleDeclared;
        info.path = path;
        return info;
        
    }

    public int getType() {
        return NAME_WITH_IMPORT_TYPE;
    }

}
