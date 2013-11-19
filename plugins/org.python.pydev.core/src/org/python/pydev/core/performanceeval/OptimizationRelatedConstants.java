/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.performanceeval;

public class OptimizationRelatedConstants {

    /**
     * This is the maximum number of code folding marks that an editor can have.
     * 
     * If the user would have more than that, no code-folding marks are shown (because
     * the UI responsiveness gets awful at that point, as eclipse suffers to keep that
     * updated).
     */
    public static final int MAXIMUM_NUMBER_OF_CODE_FOLDING_MARKS = 4000;

}
