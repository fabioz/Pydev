/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

import java.io.File;
import java.io.Serializable;

import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.StringUtils.ICallbackOnSplit;

/**
 * This class defines the key to use for some module. All its operations are based on its name.
 * The file may be null.
 * 
 * @author Fabio Zadrozny
 */
public class ModulesKey implements Comparable<ModulesKey>, Serializable {

    /**
     * 1L = just name and file
     * 2L = + zipModulePath
     */
    private static final long serialVersionUID = 2L;

    /**
     * The name is always needed!
     */
    public String name;

    /**
     * Builtins may not have the file (null)
     */
    public File file;

    /**
     * Builtins may not have the file
     */
    public ModulesKey(String name, File f) {
        this.name = name;
        this.file = f;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ModulesKey o) {
        return name.compareTo(o.name);
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ModulesKey)) {
            return false;
        }

        ModulesKey m = (ModulesKey) o;
        if (!(name.equals(m.name))) {
            return false;
        }

        //consider only the name
        return true;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        if (file != null) {
            FastStringBuffer ret = new FastStringBuffer(name, 40);
            ret.append(" - ");
            ret.appendObject(file);
            return ret.toString();
        }
        return name;
    }

    private static final class ProcessCheckIfStartingWithPart implements ICallbackOnSplit {
        private final String startingWithLowerCase;

        private ProcessCheckIfStartingWithPart(String startingWithLowerCase) {
            this.startingWithLowerCase = startingWithLowerCase;
        }

        public boolean call(String mod) {
            if (mod.length() == 0) {
                return true; //keep on going
            }
            if (mod.startsWith(startingWithLowerCase)) {
                return false; //Ok, a part starts with
            }
            return true; //keep on going
        }
    }

    /**
     * @return true if any of the parts in this modules key start with the passed string (considering the internal
     * parts lower case).
     */
    public boolean hasPartStartingWith(final String startingWithLowerCase) {
        ICallbackOnSplit onSplit = new ProcessCheckIfStartingWithPart(startingWithLowerCase);
        //Return negated: if false was returned it means it returned early or found a part.
        return !StringUtils.split(this.name.toLowerCase(), '.', onSplit);
    }

}
