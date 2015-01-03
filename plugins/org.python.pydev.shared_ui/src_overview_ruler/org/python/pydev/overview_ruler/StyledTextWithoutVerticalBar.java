/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.overview_ruler;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

public final class StyledTextWithoutVerticalBar extends StyledText {

    public StyledTextWithoutVerticalBar(Composite parent, int style) {
        super(parent, style);
    }

}