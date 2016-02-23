/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;

public class InterpretersGroup extends ElementWithChildren {

    public InterpretersGroup(ITreeElement parent) {
        super(parent);
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public void calculateChildren() {
        IInterpreterManager[] allInterpreterManagers = PydevPlugin.getAllInterpreterManagers();
        for (IInterpreterManager iInterpreterManager : allInterpreterManagers) {
            IInterpreterInfo[] interpreterInfos = iInterpreterManager.getInterpreterInfos();
            if (interpreterInfos != null && interpreterInfos.length > 0) {
                for (IInterpreterInfo iInterpreterInfo : interpreterInfos) {
                    addChild(new InterpreterGroup(this, iInterpreterInfo));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Interpreters";
    }
}
