package org.python.pydev.core;

import java.io.File;

import org.python.pydev.shared_core.string.FastStringBuffer;

public class ModulesKeyForFolder extends ModulesKey {
    /**
     * 1L = just name and file
     * 2L = + zipModulePath
     * 3L = + folderModulePath
     */
    private static final long serialVersionUID = 3L;

    public String folderModulePath;

    /**
     * Creates the module key. File may be null
     */
    public ModulesKeyForFolder(String name, File f, String folderModulePath) {
        super(name, f);
        this.folderModulePath = folderModulePath;
    }

    @Override
    public String toString() {
        FastStringBuffer ret = new FastStringBuffer(name, 40);
        if (file != null) {
            ret.append(" - ");
            ret.appendObject(file);
        }
        if (folderModulePath != null) {
            ret.append(" - folder path:");
            ret.append(folderModulePath);
        }
        return ret.toString();
    }

    @Override
    public void toIO(FastStringBuffer buf) {
        super.toIO(buf);
        buf.append('|').append(folderModulePath).append('|').append('0');
    }
}