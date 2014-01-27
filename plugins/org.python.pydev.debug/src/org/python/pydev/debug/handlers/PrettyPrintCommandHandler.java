/******************************************************************************
* Copyright (C) 2013  Jonah Graham
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.debug.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.IVariableLocator;
import org.python.pydev.debug.model.remote.RunCustomOperationCommand;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Pretty Print the current selection is possible.
 */
public class PrettyPrintCommandHandler extends AbstractHandler {
    private static final String PPRINT_CODE = "from pprint import pprint";
    private static final String PPRINT_FUNCTION = "pprint";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        Tuple<AbstractDebugTarget, IVariableLocator> context = RunCustomOperationCommand
                .extractContextFromSelection(selection);
        if (context != null) {
            RunCustomOperationCommand cmd = new RunCustomOperationCommand(context.o1, context.o2, PPRINT_CODE,
                    PPRINT_FUNCTION);
            context.o1.postCommand(cmd);
        }

        return null;
    }
}
