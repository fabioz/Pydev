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

import java.io.Serializable;

import org.python.pydev.ast.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.core.IInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ObjectsInternPool;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.StringUtils;

public abstract class AbstractInfo implements IInfo, Serializable {
    /**
     * Changed for 2.1
     */
    private static final long serialVersionUID = 3L;

    /**
     * the name
     */
    public final String name;

    /**
     * This is the path (may be null)
     */
    public final String path;

    /**
     * the name of the module where this function is declared
     */
    public final String moduleDeclared;

    public final IPythonNature nature;

    public final String file;

    // 0-based
    public final int line;

    // 0-based
    public final int col;

    public AbstractInfo(String name, String moduleDeclared, String path, IPythonNature nature, String file, int line,
            int col) {
        if (line < 0 || col < 0) {
            if (file != null && PythonPathHelper.isValidSourceFile(file)) {
                Log.log(StringUtils.format("Not expecting to get negative line for: %s - %s - %s - %s - %s - %s", name,
                        moduleDeclared, path, file, line, col));
            }
            if (line < 0) {
                line = 0;
            }
            if (col < 0) {
                col = 0;
            }
        }
        synchronized (ObjectsInternPool.lock) {
            this.name = ObjectsInternPool.internUnsynched(name);
            this.moduleDeclared = ObjectsInternPool.internUnsynched(moduleDeclared);
            this.path = ObjectsInternPool.internUnsynched(path);
            this.file = ObjectsInternPool.internUnsynched(file);
            this.line = line;
            this.col = col;
        }
        this.nature = nature;
    }

    /**
     * Same as the other constructor but does not intern anything.
     */
    public AbstractInfo(String name, String moduleDeclared, String path, boolean doNotInternOnThisContstruct,
            IPythonNature nature, String file, int line, int col) {
        if (line < 0 || col < 0) {
            if (file != null && PythonPathHelper.isValidSourceFile(file)) {
                Log.log(StringUtils.format("Not expecting to get negative line for: %s - %s - %s - %s - %s - %s", name,
                        moduleDeclared, path, file, line, col));
            }
            if (line < 0) {
                line = 0;
            }
            if (col < 0) {
                col = 0;
            }
        }
        this.name = name;
        this.moduleDeclared = moduleDeclared;
        this.path = path;
        this.nature = nature;
        this.file = file;
        this.line = line;
        this.col = col;
    }

    @Override
    public IPythonNature getNature() {
        return nature;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDeclaringModuleName() {
        return moduleDeclared;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getFile() {
        return file;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getCol() {
        return col;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IInfo)) {
            return false;
        }
        IInfo otherInfo = (IInfo) obj;

        if (otherInfo.getType() != getType()) {
            return false;
        }

        if (!otherInfo.getDeclaringModuleName().equals(this.moduleDeclared)) {
            return false;
        }

        if (!otherInfo.getName().equals(this.name)) {
            return false;
        }

        //if one of them is null, the other must also be null...
        String otherPath = otherInfo.getPath();
        String myPath = getPath();
        if ((otherPath == null || myPath == null)) {
            if (otherPath != myPath) {
                //one of them is not null
                return false;
            }
            //both are null
            return true;
        }

        //they're not null
        if (!otherPath.equals(myPath)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return 7 * this.name.hashCode() + this.moduleDeclared.hashCode() * getType();
    }

    @Override
    public String toString() {
        return this.name + " (" + this.moduleDeclared + ") - Path:" + getPath();
    }

    @Override
    public int compareTo(IInfo o) {
        int r = name.compareTo(o.getName());
        if (r != 0) {
            return r;
        }
        return moduleDeclared.compareTo(o.getDeclaringModuleName());
    }
}
