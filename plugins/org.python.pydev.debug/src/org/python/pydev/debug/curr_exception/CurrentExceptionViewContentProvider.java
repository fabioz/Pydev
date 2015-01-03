/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.curr_exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.CaughtException;
import org.python.pydev.debug.model.PyVariable;

public class CurrentExceptionViewContentProvider implements ITreeContentProvider {

    private final Map<Object, Object> parentCache = new HashMap<>();

    @Override
    public void dispose() {
        parentCache.clear();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        parentCache.clear();
    }

    @Override
    public Object[] getElements(Object inputElement) {
        List<IDebugTarget> elements = (List<IDebugTarget>) inputElement;
        for (IDebugTarget iDebugTarget : elements) {
            getChildren(iDebugTarget); //just to make sure we'll cache this level in the parentCache.
        }
        return elements.toArray(new IDebugTarget[elements.size()]);
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        Object[] children = internalGetChildren(parentElement);
        if (children != null) {
            for (Object child : children) {
                parentCache.put(child, parentElement);
            }
        }
        return children;
    }

    private Object[] internalGetChildren(Object parentElement) {
        if (parentElement instanceof AbstractDebugTarget) {
            AbstractDebugTarget target = (AbstractDebugTarget) parentElement;
            List<CaughtException> currExceptions = target.getCurrExceptions();
            return currExceptions.toArray(new CaughtException[currExceptions.size()]);
        }

        if (parentElement instanceof CaughtException) {
            CaughtException caughtException = (CaughtException) parentElement;
            return caughtException.threadNstack.stack;

        }
        if (parentElement instanceof IThread) {
            IThread pyThread = (IThread) parentElement;
            try {
                return pyThread.getStackFrames();
            } catch (DebugException e) {
                Log.log(e);
                return null;
            }
        }

        if (parentElement instanceof IStackFrame) {
            IStackFrame iStackFrame = (IStackFrame) parentElement;
            try {
                return iStackFrame.getVariables();
            } catch (DebugException e) {
                Log.log(e);
                return null;
            }
        }

        if (parentElement instanceof PyVariable) {
            PyVariable pyVariable = (PyVariable) parentElement;
            try {
                return pyVariable.getVariables();
            } catch (DebugException e) {
                Log.log(e);
                return null;
            }
        }

        Log.log("Unexpected parent: " + parentElement);
        return null;
    }

    @Override
    public Object getParent(Object element) {
        Object parent = parentCache.get(element);
        return parent;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof List) {
            List list = (List) element;
            return list.size() > 0;
        }
        if (element instanceof IValue) {
            try {
                return ((IValue) element).hasVariables();
            } catch (DebugException e) {
                Log.log(e);
            }
        }
        return true;
    }

}
