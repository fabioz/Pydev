/**
 * Copyright (c) 2014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.shared_core.string.TextSelectionUtils;

public class TextVerticalLinesIndentGuide implements IVerticalLinesIndentGuideComputer {

    private ITabWidthProvider tabWidthProvider;

    public TextVerticalLinesIndentGuide(ITabWidthProvider tabWidthProvider) {
        this.tabWidthProvider = tabWidthProvider;
    }

    public int getTabWidth() {
        return tabWidthProvider.getTabWidth();
    }

    @Override
    public SortedMap<Integer, List<VerticalLinesToDraw>> computeVerticalLinesToDrawInRegion(
            StyledText styledText, int topIndex, int bottomIndex) {

        SortedMap<Integer, List<VerticalLinesToDraw>> lineToVerticalLinesToDraw = new TreeMap<Integer, List<VerticalLinesToDraw>>();
        int lineHeight = styledText.getLineHeight();
        int lineCount = styledText.getLineCount();
        if (bottomIndex > lineCount - 1) {
            bottomIndex = lineCount - 1;
        }
        // lineHeight = styledText.getLinePixel(1) - styledText.getLinePixel(0);

        for (int line = topIndex; line <= bottomIndex; line++) {
            // Only draw in visible range... (topIndex/bottomIndex)

            final String string = styledText.getLine(line);

            int firstCharPosition = TextSelectionUtils.getFirstCharPosition(string);

            if (firstCharPosition == string.length()) {
                // The line only has whitespaces... Let's copy the indentation guide from the previous line (if any)
                // just updating the y.
                List<VerticalLinesToDraw> previousLine = lineToVerticalLinesToDraw.get(line - 1);
                if (previousLine != null) {
                    ArrayList<VerticalLinesToDraw> newLst = new ArrayList<>(previousLine.size());
                    for (VerticalLinesToDraw verticalLinesToDraw : previousLine) {
                        newLst.add(verticalLinesToDraw.copyChangingYOffset(lineHeight));
                    }
                    lineToVerticalLinesToDraw.put(line, newLst);
                }
                continue;
            }

            if (firstCharPosition == 0) {
                continue;
            }

            int lineOffset = -1;

            String spaces = string.substring(0, firstCharPosition);
            int level = 0;
            int whitespacesFound = 0;
            int tabWidthUsed = getTabWidth();
            for (int j = 0; j < firstCharPosition - 1; j++) { //-1 because we don't want to cover for the column where a non whitespace char is.
                char c = spaces.charAt(j);
                if (c == '\t') {
                    level++;
                    whitespacesFound = 0;
                } else {
                    //whitespace (not tab)
                    whitespacesFound++;
                    if (whitespacesFound % tabWidthUsed == 0) {
                        level++;
                        whitespacesFound = 0;
                    }
                }
                if (level > 0) {
                    Point point1;

                    if (lineOffset == -1) {
                        lineOffset = styledText.getOffsetAtLine(line);
                    }
                    point1 = styledText.getLocationAtOffset(lineOffset + j + 1);
                    int xCoord = point1.x + 3;

                    VerticalLinesToDraw verticalLinesToDraw = new VerticalLinesToDraw(xCoord,
                            point1.y, xCoord,
                            point1.y + lineHeight, line);

                    List<VerticalLinesToDraw> lst = lineToVerticalLinesToDraw.get(line);
                    if (lst == null) {
                        lst = new ArrayList<VerticalLinesToDraw>();
                        lineToVerticalLinesToDraw.put(line, lst);
                    }
                    lst.add(verticalLinesToDraw);

                    level--;
                }
            }
        }
        return lineToVerticalLinesToDraw;
    }

}
