/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;

/**
 * An empty module representing a path in a zip file.
 * 
 * @author Fabio
 */
public class EmptyModuleForZip extends EmptyModule{

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

}
