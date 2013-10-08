/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 23, 2006
 * @author Fabio
 */
package org.python.pydev.debug.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.core.log.Log;


/**
 * This class should check for value modifications in the stacks while debugging.
 * Its public interface is completely synchronized.
 * 
 * @author Fabio
 */
public class ValueModificationChecker {

    /**
     * compares stack frames to check for modified variables (and mark them as modified in the new stack)
     * 
     * @param newFrame the new frame
     * @param oldFrame the old frame
     */
    public void verifyVariablesModified(IVariable[] newFrameVariables, IVariable[] oldVariables) {
        if (oldVariables == null) {
            return; //All variables are new, so, no point in notifying it.
        }
        PyVariable newVariable = null;

        try {
            HashMap<String, IVariable> map = new HashMap<String, IVariable>();
            for (IVariable var : oldVariables) {
                map.put(var.getName(), var);
            }
            Map<String, IVariable> variablesAsMap = map;

            //we have to check for each new variable
            for (int i = 0; i < newFrameVariables.length; i++) {
                newVariable = (PyVariable) newFrameVariables[i];

                PyVariable oldVariable = (PyVariable) variablesAsMap.get(newVariable.getName());

                if (oldVariable != null) {
                    boolean equals = newVariable.getValueString().equals(oldVariable.getValueString());

                    //if it is not equal, it was modified
                    newVariable.setModified(!equals);

                } else { //it didn't exist before...
                    newVariable.setModified(true);
                }
            }

        } catch (DebugException e) {
            Log.log(e);
        }
    }

    /**
     * Thread id --> stack id --> stack
     */
    private Map<String, Map<String, PyStackFrame>> cache = new HashMap<String, Map<String, PyStackFrame>>();
    private Object lock = new Object();

    /**
     * Synched access to verify the modification
     * @param frame the frame that we're checking
     * @param newFrameVariables the variables that were added to the stack.
     */
    public synchronized void verifyModified(PyStackFrame frame, IVariable[] newFrameVariables) {
        synchronized (lock) {
            Map<String, PyStackFrame> threadIdCache = cache.get(frame.getThreadId());
            if (threadIdCache == null) {
                threadIdCache = new HashMap<String, PyStackFrame>();
                cache.put(frame.getThreadId(), threadIdCache);
            }

            PyStackFrame cacheFrame = threadIdCache.get(frame.getId());
            if (cacheFrame == null) {
                threadIdCache.put(frame.getId(), frame);
                return;
            }
            //not null
            if (cacheFrame == frame) { //if is same, it has already been checked.
                return;
            }

            //if it is not the same, we have to check it and mark it as the new frame.
            verifyVariablesModified(newFrameVariables, cacheFrame.getInternalVariables());
            threadIdCache.put(frame.getId(), frame);
        }
    }

    /**
     * Removes from the cache all the threads that are not present in the threads passed.
     */
    public synchronized void onlyLeaveThreads(PyThread[] threads) {
        synchronized (lock) {
            HashSet<String> ids = new HashSet<String>();
            for (PyThread thread : threads) {
                ids.add(thread.getId());
            }
            //Must iterate in a copy.
            Set<String> keySet = new HashSet<String>(cache.keySet());
            for (String id : keySet) {
                if (!ids.contains(id)) {
                    cache.remove(id);
                }
            }
        }
    }

}
