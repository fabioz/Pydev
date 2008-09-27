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
