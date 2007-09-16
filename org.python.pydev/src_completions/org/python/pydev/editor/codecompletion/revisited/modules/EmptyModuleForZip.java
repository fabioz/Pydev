package org.python.pydev.editor.codecompletion.revisited.modules;

import java.io.File;

/**
 * An empty module representing a path in a zip file.
 * 
 * @author Fabio
 */
public class EmptyModuleForZip extends EmptyModule{

    public String pathInZip;

    public EmptyModuleForZip(String name, File f, String pathInZip) {
        super(name, f);
        this.pathInZip = pathInZip;
    }

}
