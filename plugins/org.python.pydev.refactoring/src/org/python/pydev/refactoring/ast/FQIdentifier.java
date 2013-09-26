/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast;

public class FQIdentifier {

    private String module;

    private String realName;

    private String alias;

    public FQIdentifier(String module, String realName, String alias) {
        this.module = module;
        this.realName = realName;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public String getModule() {
        return module;
    }

    public String getRealName() {
        return realName;
    }

    public String getFQName() {
        return module + "." + realName;
    }

    public String getProbableModuleName() {
        int offset = getRealName().indexOf(".");
        if (offset > 1) {
            return getModule() + "." + getRealName().substring(0, offset);
        }
        return getModule();
    }

}
