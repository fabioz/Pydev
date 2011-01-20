/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.analysis.additionalinfo;

import org.python.pydev.core.ObjectsPool;


public class AttrInfo extends AbstractInfo{

    private static final long serialVersionUID = 1L;

    /**
     * We create it directly from the name of the assign here, as opposed to other places that
     * really have the AST, because:
     * - we may have multiple targets in an assign
     * - we lack all the info to determine if this is a valid assign (we have to see if it is a global
     * or a class or an instance attr).
     */
    public static AttrInfo fromAssign(String def, String moduleDeclared, String path) {
        AttrInfo info = new AttrInfo();
        info.name = ObjectsPool.intern(def);
        info.moduleDeclared = moduleDeclared;
        info.path = path;
        return info;
        
    }

    public int getType() {
        return ATTRIBUTE_WITH_IMPORT_TYPE;
    }

}
