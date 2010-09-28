package com.python.pydev.analysis.indexview;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;

public class InterpretersGroup extends ElementWithChildren{

    public InterpretersGroup(ITreeElement parent) {
        super(parent);
    }

    public boolean hasChildren() {
        return true;
    }

    @Override
    public void calculateChildren() {
        IInterpreterManager[] allInterpreterManagers = PydevPlugin.getAllInterpreterManagers();
        for (IInterpreterManager iInterpreterManager : allInterpreterManagers) {
            IInterpreterInfo[] interpreterInfos = iInterpreterManager.getInterpreterInfos();
            if(interpreterInfos != null && interpreterInfos.length > 0){
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
