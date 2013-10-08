/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonNature;

public class CompletionStateFactory {

    /**
     * @return a default completion state for globals (empty act. token)
     */
    public static ICompletionState getEmptyCompletionState(IPythonNature nature, ICompletionCache completionCache) {
        return new CompletionState(-1, -1, "", nature, "", completionCache);
    }

    /**
     * @return a default completion state for globals (act token defined)
     */
    public static ICompletionState getEmptyCompletionState(String token, IPythonNature nature,
            ICompletionCache completionCache) {
        return new CompletionState(-1, -1, token, nature, "", completionCache);
    }

    /**
     * @param line: start at 0
     * @param col: start at 0
     * @return a default completion state for globals (act token defined)
     */
    public static ICompletionState getEmptyCompletionState(String token, IPythonNature nature, int line, int col,
            ICompletionCache completionCache) {
        return new CompletionState(line, col, token, nature, "", completionCache);
    }

}
