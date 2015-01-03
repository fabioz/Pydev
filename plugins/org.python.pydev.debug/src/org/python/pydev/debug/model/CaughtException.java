/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IStackFrame;
import org.python.pydev.core.docutils.StringEscapeUtils;
import org.python.pydev.debug.model.XMLUtils.StoppedStack;

public class CaughtException implements IAdaptable {

    public final String excType;
    public final String msg;
    public final StoppedStack threadNstack;
    public final String currentFrameId;

    public CaughtException(String currentFrameId, String excType, String msg, StoppedStack threadNstack) {
        this.currentFrameId = currentFrameId;
        this.excType = StringEscapeUtils.unescapeXml(excType);
        this.msg = StringEscapeUtils.unescapeXml(msg);
        this.threadNstack = threadNstack;
        IStackFrame[] stack = threadNstack.stack;
        for (IStackFrame iStackFrame : stack) {
            if (iStackFrame instanceof PyStackFrame) {
                PyStackFrame f = (PyStackFrame) iStackFrame;
                if (currentFrameId.equals(f.getId())) {
                    f.setCurrentStackFrame();
                    break;
                }
            }
        }
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == IStackFrame.class) {
            IStackFrame[] stack = this.threadNstack.stack;
            if (stack != null && stack.length > 0) {
                return stack[0];
            }
        }
        return null;
    }

}