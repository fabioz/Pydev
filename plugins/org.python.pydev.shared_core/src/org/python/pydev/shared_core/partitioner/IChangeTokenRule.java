/**
 * Copyright (c) 2013-2015 by Fabio Zadrozny. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.partitioner;

import org.eclipse.jface.text.rules.IToken;

public interface IChangeTokenRule {

    void setToken(IToken token);

}
