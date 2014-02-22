/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;

import org.python.pydev.shared_core.string.StringUtils;

/**
 * An empty module representing a path in a zip file.
 * 
 * @author Fabio
 */
public class EmptyModuleForZip extends EmptyModule {

    private static final long serialVersionUID = 1L;

    public String pathInZip;

    /**
     * Determines if it was found as a file within the zip (otherwise, it's a folder).
     */
    public boolean isFile;

    public EmptyModuleForZip(String name, File f, String pathInZip, boolean isFile) {
        super(name, f);
        this.pathInZip = pathInZip;
        this.isFile = isFile;
    }

    @Override
    public String toString() {
        return StringUtils.join(" ", "EmptyModuleForZip[", name, " file: ", f, " path: ", pathInZip, " isFile: ",
                isFile, "]");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EmptyModuleForZip)) {
            return false;
        }
        EmptyModuleForZip m = (EmptyModuleForZip) obj;

        if (name == null || m.name == null) {
            if (name != m.name) {
                return false;
            }
            //both null at this point
        } else if (!name.equals(m.name)) {
            return false;
        }

        if (f == null || m.f == null) {
            if (f != m.f) {
                return false;
            }
            //both null at this point
        } else if (!f.equals(m.f)) {
            return false;
        }
        if (pathInZip == null || m.pathInZip == null) {
            if (pathInZip != m.pathInZip) {
                return false;
            }
            //both null at this point
        } else if (!pathInZip.equals(m.pathInZip)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 123;
        if (f != null) {
            hash += f.hashCode();
        }
        if (name != null) {
            hash += name.hashCode();
        }
        if (pathInZip != null) {
            hash += pathInZip.hashCode();
        }
        return hash;
    }
}
