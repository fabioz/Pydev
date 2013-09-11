/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 2, 2006
 */
package org.python.pydev.editor;

import org.eclipse.jface.action.IAction;

public class ActionInfo {
    public final IAction action;
    public final String description;
    public final String binding;
    public final boolean needsEnter;

    public ActionInfo(IAction action, String description, String binding, boolean needsEnter) {
        this.action = action;
        this.description = description;
        this.binding = binding.toLowerCase();
        this.needsEnter = needsEnter;
    }
}