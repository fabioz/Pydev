/**
 * Copyright (c) 2014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.shared_core.string.TextSelectionUtils;

public class TextVerticalLinesIndentGuide implements IVerticalLinesIndentGuideComputer {

    private final IVerticalIndentGuidePreferencesProvider verticalIndentPrefs;

    public TextVerticalLinesIndentGuide(IVerticalIndentGuidePreferencesProvider verticalIndentPrefs) {
        Assert.isNotNull(verticalIndentPrefs);
        this.verticalIndentPrefs = verticalIndentPrefs;
    }

    @Override
    public int getTabWidth() {
        return verticalIndentPrefs.getTabWidth();
    }

    @Override
    public boolean getShowIndentGuide() {
        return verticalIndentPrefs.getShowIndentGuide();
    }

    @Override
    public Color getColor(StyledText styledText) {
        return verticalIndentPrefs.getColor(styledText);
    }

    @Override
    public int getTransparency() {
        return verticalIndentPrefs.getTransparency();
    }

    @Override
    public void dispose() {
        verticalIndentPrefs.dispose();
    }

    @Override
    public SortedMap<Integer, List<VerticalLinesToDraw>> computeVerticalLinesToDrawInRegion(
            StyledText styledText, int topIndex, int bottomIndex) {
        final int originalTopIndex = topIndex;

        SortedMap<Integer, List<VerticalLinesToDraw>> lineToVerticalLinesToDraw;
        lineToVerticalLinesToDraw = new TreeMap<Integer, List<VerticalLinesToDraw>>();
        int lineHeight = styledText.getLineHeight();
        int lineCount = styledText.getLineCount();
        if (bottomIndex > lineCount - 1) {
            bottomIndex = lineCount - 1;
        }
        // lineHeight = styledText.getLinePixel(1) - styledText.getLinePixel(0);

        // Note: if the top index is an all whitespace line, we have to start computing earlier to have something valid at the all whitespaces line
        while (topIndex > 0) {
            final String string = styledText.getLine(topIndex);
            int firstCharPosition = TextSelectionUtils.getFirstCharPosition(string);
            if (firstCharPosition == string.length()) {
                // All whitespaces... go back until we find one that is not only whitespaces.
                topIndex--;
            } else {
                break;
            }
        }

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

            computeLine(string, firstCharPosition, styledText, line, lineHeight, lineToVerticalLinesToDraw);
        }
        if (originalTopIndex != topIndex) {
            // Remove the entries we created just because we had to generate based on previous lines (those shouldn't be drawn:
            // we only want the visible region in the return).
            Set<Entry<Integer, List<VerticalLinesToDraw>>> entrySet = lineToVerticalLinesToDraw.entrySet();
            Iterator<Entry<Integer, List<VerticalLinesToDraw>>> iterator = entrySet.iterator();
            while (iterator.hasNext()) {
                Entry<Integer, List<VerticalLinesToDraw>> next = iterator.next();
                if (next.getKey() < originalTopIndex) {
                    iterator.remove();
                } else {
                    break; //As it's sorted, we know we can bail out early.
                }
            }
        }
        return lineToVerticalLinesToDraw;
    }

    private void computeLine(String string, int firstCharPosition, StyledText styledText, int line, int lineHeight,
            SortedMap<Integer, List<VerticalLinesToDraw>> lineToVerticalLinesToDraw) {
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
                        point1.y, xCoord, point1.y + lineHeight);

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

}
