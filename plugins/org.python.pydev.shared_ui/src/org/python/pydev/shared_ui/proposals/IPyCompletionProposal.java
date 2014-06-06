/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 25, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.shared_ui.proposals;

/**
 * @author Fabio Zadrozny
 */
public interface IPyCompletionProposal {

    int PRIORITY_LOCALS = -1;

    //those have local priorities, but for some reason have a lower priority than locals
    int PRIORITY_LOCALS_1 = 0;
    int PRIORITY_LOCALS_2 = 1;

    int PRIORITY_CREATE = 5;
    int PRIORITY_DEFAULT = 10;
    int PRIORTTY_IPYTHON_MAGIC = 25;

    int PRIORITY_GLOBALS_EXACT = 40;
    int PRIORITY_PACKAGES_EXACT = 41;

    int PRIORITY_GLOBALS = 50;
    int PRIORITY_PACKAGES = 100;

    /**
     * @return the priority for showing this completion proposal, so that lower priorities are
     * shown earlier in the list.
     */
    public int getPriority();
}
