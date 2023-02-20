package org.python.pydev.core;

import java.io.File;

import org.python.pydev.shared_core.string.FastStringBuffer;

public class ModulesKeyForFolder extends ModulesKey {

    private static final long serialVersionUID = 1L;

    public ModulesKeyForFolder(String name, File f) {
        super(name, f);
    }

    @Override
    public void toIO(FastStringBuffer buf) {
        super.toIO(buf);
        buf.append("|^");
    }
}