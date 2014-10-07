/**
 * Copyright (c) 2014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor;

import java.util.List;
import java.util.SortedMap;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;

public interface IVerticalLinesIndentGuideComputer {

    SortedMap<Integer, List<VerticalLinesToDraw>> computeVerticalLinesToDrawInRegion(StyledText styledText,
            int topIndex,
            int bottomIndex);

    int getTabWidth();

    boolean getShowIndentGuide();

    void dispose();

    Color getColor(StyledText styledText);

    int getTransparency();

}
