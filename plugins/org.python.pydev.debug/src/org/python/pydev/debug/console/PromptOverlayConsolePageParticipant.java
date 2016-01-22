/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.console;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.eclipse.ui.part.IPageBookViewPage;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.newconsole.CurrentPyStackFrameForConsole;

@SuppressWarnings("restriction")
public class PromptOverlayConsolePageParticipant implements IConsolePageParticipant {

    private PromptOverlay promptOverlay;

    @Override
    public Object getAdapter(Class adapter) {
        return null;
    }

    @Override
    public void init(IPageBookViewPage page, IConsole console) {
        if (!(console instanceof ProcessConsole)) {
            return;
        }
        ProcessConsole processConsole = (ProcessConsole) console;
        IProcess process = processConsole.getProcess();
        if (process == null) {
            return;
        }

        String attribute = process.getAttribute(Constants.PYDEV_DEBUG_IPROCESS_ATTR);
        if (!Constants.PYDEV_DEBUG_IPROCESS_ATTR_TRUE.equals(attribute)) {
            //Only provide the console page
            return;
        }
        if (page instanceof IOConsolePage) {
            final CurrentPyStackFrameForConsole currentPyStackFrameForConsole = new CurrentPyStackFrameForConsole(
                    console);
            IOConsolePage consolePage = (IOConsolePage) page;
            this.promptOverlay = new PromptOverlay(consolePage, processConsole, currentPyStackFrameForConsole);
        }

    }

    @Override
    public void dispose() {
        if (this.promptOverlay != null) {
            this.promptOverlay.dispose();
        }
        this.promptOverlay = null;
    }

    @Override
    public void activated() {
        if (this.promptOverlay != null) {
            this.promptOverlay.activated();
        }
    }

    @Override
    public void deactivated() {
        if (this.promptOverlay != null) {
            this.promptOverlay.deactivated();
        }
    }

}
