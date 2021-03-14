package org.python.pydev.shared_core.code_completion;
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

/**
 * @author Fabio Zadrozny
 */
public interface IPyCompletionProposal {

    int LOWER_PRIORITY = -100;
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
     * Defines a 'regular' apply, in which we add the completion as usual
     */
    int ON_APPLY_DEFAULT = 1;

    /**
     * Defines that when applying the changes we should just show the context info and do no other change
     */
    int ON_APPLY_JUST_SHOW_CTX_INFO = 2;

    /**
     * Defines that we should add only the parameters on the apply and show the context info too
     */
    int ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS = 3;

    /**
     * @return the priority for showing this completion proposal, so that lower priorities are
     * shown earlier in the list.
     */
    public int getPriority();

    public static interface ICompareContext {

        int SAME_PROJECT_PRIORITY = 1;
        int ANY_PROJECT_PRIORITY = 2;
        int DEFAULT_PRIORITY = 3;

        /**
         * @param compareContext may be null
         * @return
         */
        public int getPriorityRelatedTo(ICompareContext compareContext);
    }

    public ICompareContext getCompareContext();

    int BEHAVIOR_OVERRIDES = 0;

    int BEHAVIOR_COEXISTS = 1;

    int BEHAVIOR_IS_OVERRIDEN = 2;

    public int getOverrideBehavior(ICompletionProposalHandle curr);

}
