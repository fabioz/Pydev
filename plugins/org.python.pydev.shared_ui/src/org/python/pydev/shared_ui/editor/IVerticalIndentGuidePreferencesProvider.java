/**
 * Copyright (c) 2014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;

public interface IVerticalIndentGuidePreferencesProvider {

    public boolean getShowIndentGuide();

    public int getTabWidth();

    public void dispose();

    public Color getColor(StyledText styledText);

    public int getTransparency(); //0-255
}
