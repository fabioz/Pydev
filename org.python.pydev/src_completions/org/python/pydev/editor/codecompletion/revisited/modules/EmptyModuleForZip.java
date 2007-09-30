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
