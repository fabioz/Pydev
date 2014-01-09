/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;
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

    /**
     * Note that we'll have to check what we have internally against the current information in the settings for each
     * interpreter (as this information may be just part of what's in the settings).
     */
    public boolean somethingChanged() {
        ManagerInfoToUpdate currentInfoInSettings = new ManagerInfoToUpdate(PydevPlugin
                .getInterpreterManagerToInterpreterNameToInfo());

        int len = managerAndInfos.length;
        for (int i = 0; i < len; i++) {
            Tuple<IInterpreterManager, IInterpreterInfo> tup1 = managerAndInfos[i];

            boolean foundMatching = false;
            for (Tuple<IInterpreterManager, IInterpreterInfo> tup2 : currentInfoInSettings.managerAndInfos) {
                if (tup1.o1 == tup2.o1) {
                    if (tup1.o2.getName().equals(tup2.o2.getName())) {
                        if (tup1.o2.getModificationStamp() == tup2.o2.getModificationStamp()) {
                            foundMatching = true;
                            break; //break inner for
                        }
                    }
                }
            }

            if (!foundMatching) {
                if (SynchSystemModulesManager.DEBUG) {
                    System.out.println("Did not find match for: " + tup1.o2.getName());
                }
                return true; //if we didn't find a match, something changed
            }
        }
        return false; //we found matches (including time) for all infos, so, nothing changed.
    }

}
