/**
 * Copyright (c) 2017 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion;

import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPythonNature;

/**
 */
public interface IPyDevCompletionParticipant3 {

    IDefinition findDefinitionForMethodParameter(Definition d, IPythonNature nature, ICompletionState completionCache);

}
