/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

import java.util.Iterator;
import java.util.Set;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class InterpreterGroup extends ElementWithChildren {

    private InterpreterInfo interpreterInfo;

    public InterpreterGroup(ITreeElement parent, IInterpreterInfo interpreterInfo) {
        super(parent);
        this.interpreterInfo = (InterpreterInfo) interpreterInfo;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    protected void calculateChildren() {
        Iterator<String> forcedLibsIterator = this.interpreterInfo.forcedLibsIterator();
        while (forcedLibsIterator.hasNext()) {
            addChild(new ForcedLibGroup(this, this.interpreterInfo, forcedLibsIterator.next()));
        }

        ISystemModulesManager modulesManager = this.interpreterInfo.getModulesManager();
        Set<String> allModuleNames = modulesManager.getAllModuleNames(false, "");
        for (String moduleName : allModuleNames) {
            addChild(new LeafElement(this, moduleName));
        }
    }

    @Override
    public String toString() {
        return this.interpreterInfo.getNameForUI();
    }

}
