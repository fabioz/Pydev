package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.shared_core.structure.Tuple;

public class ManagerInfoToUpdate {

    private final Tuple<IInterpreterManager, IInterpreterInfo>[] managerAndInfos;

    @SuppressWarnings("unchecked")
    public ManagerInfoToUpdate(Map<IInterpreterManager, Map<String, IInterpreterInfo>> managerToNameToInfo) {
        ArrayList<Object> lst = new ArrayList<>();
        for (Entry<IInterpreterManager, Map<String, IInterpreterInfo>> entry : managerToNameToInfo
                .entrySet()) {
            for (Entry<String, IInterpreterInfo> entry2 : entry.getValue().entrySet()) {
                lst.add(new Tuple<IInterpreterManager, IInterpreterInfo>(entry.getKey(), entry2.getValue()));
                if (SynchSystemModulesManager.DEBUG) {
                    System.out.println("Will check: " + entry2.getKey());
                }
            }
        }
        managerAndInfos = lst.toArray(new Tuple[lst.size()]);
    }

    public Tuple<IInterpreterManager, IInterpreterInfo>[] getManagerAndInfos() {
        return managerAndInfos;
    }

}
