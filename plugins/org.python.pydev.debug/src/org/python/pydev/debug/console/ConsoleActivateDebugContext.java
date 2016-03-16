/******************************************************************************
* Copyright (C) 2012  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.console;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.part.IPageBookViewPage;
import org.python.pydev.debug.newconsole.PydevConsole;

public class ConsoleActivateDebugContext implements IConsolePageParticipant, IDebugContextListener {
    private PydevConsole console;

    private IPageBookViewPage page;
    private IConsoleView view;

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    protected IProcess getProcess() {
        return console != null ? console.getProcess() : null;
    }

    @Override
    public void debugContextChanged(DebugContextEvent event) {
        if ((event.getFlags() & DebugContextEvent.ACTIVATED) > 0) {
            if (view != null && getProcess() != null && getProcess().equals(DebugUITools.getCurrentProcess())) {
                view.display(console);
            }
        }

    }

    @Override
    public void init(IPageBookViewPage page, IConsole console) {
        this.page = page;
        this.console = (PydevConsole) console;

        view = (IConsoleView) page.getSite().getPage().findView(IConsoleConstants.ID_CONSOLE_VIEW);
        DebugUITools.getDebugContextManager().getContextService(page.getSite().getWorkbenchWindow())
                .addDebugContextListener(this);
    }

    @Override
    public void dispose() {
        DebugUITools.getDebugContextManager().getContextService(this.page.getSite().getWorkbenchWindow())
                .removeDebugContextListener(this);
        console = null;
    }

    @Override
    public void activated() {
    }

    @Override
    public void deactivated() {
    }

}
