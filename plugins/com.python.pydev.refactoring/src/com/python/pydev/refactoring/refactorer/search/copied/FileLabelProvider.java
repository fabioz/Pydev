/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search.copied;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.search.SearchMessages;

public class FileLabelProvider extends LabelProvider {

    public static final int SHOW_LABEL = 1;
    public static final int SHOW_LABEL_PATH = 2;
    public static final int SHOW_PATH_LABEL = 3;

    private static final String fgSeparatorFormat = "{0} - {1}"; //$NON-NLS-1$

    private static final String fgEllipses = " ... "; //$NON-NLS-1$

    private final WorkbenchLabelProvider fLabelProvider;
    private final AbstractTextSearchViewPage fPage;
    private final Comparator<FileMatch> fMatchComparator;

    private final Image fLineMatchImage;

    private int fOrder;

    public FileLabelProvider(AbstractTextSearchViewPage page, int orderFlag) {
        fLabelProvider = new WorkbenchLabelProvider();
        fOrder = orderFlag;
        fPage = page;

        fLineMatchImage = SharedUiPlugin.getImageCache().get(UIConstants.LINE_MATCH);
        fMatchComparator = new Comparator<FileMatch>() {
            public int compare(FileMatch o1, FileMatch o2) {
                return o1.getOriginalOffset() - o2.getOriginalOffset();
            }
        };
    }

    public void setOrder(int orderFlag) {
        fOrder = orderFlag;
    }

    public int getOrder() {
        return fOrder;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText(Object object) {
        return getStyledText(object);
    }

    public String getStyledText(Object element) {
        if (element instanceof LineElement) {
            return getLineElementLabel((LineElement) element);
        }

        if (!(element instanceof IResource)) {
            return new String();
        }

        IResource resource = (IResource) element;
        if (!resource.exists()) {
            new String(SearchMessages.FileLabelProvider_removed_resource_label);
        }

        String name = BasicElementLabels.getResourceName(resource);
        if (fOrder == SHOW_LABEL) {
            return getColoredLabelWithCounts(resource, new String(name));
        }

        String pathString = BasicElementLabels.getPathLabel(resource.getParent().getFullPath(), false);
        if (fOrder == SHOW_LABEL_PATH) {
            String str = new String(name);
            //          String decorated = Messages.format(fgSeparatorFormat, new String[] { str, pathString });

            //          decorateColoredString(str, decorated, String.QUALIFIER_STYLER);
            return getColoredLabelWithCounts(resource, str);
        }

        String str = new String(MessageFormat.format(fgSeparatorFormat, pathString, name));
        return getColoredLabelWithCounts(resource, str);
    }

    private String getLineElementLabel(LineElement lineElement) {
        int lineNumber = lineElement.getLine();
        String lineNumberString = MessageFormat
                .format(SearchMessages.FileLabelProvider_line_number, new Integer(lineNumber));

        String str = new String(lineNumberString);

        FileMatch[] matches = lineElement.getMatches(fPage.getInput());
        Arrays.sort(matches, fMatchComparator);

        String content = lineElement.getContents();

        int pos = evaluateLineStart(matches, content, lineElement.getOffset());

        int length = content.length();

        int charsToCut = getCharsToCut(length, matches); // number of characters to leave away if the line is too long
        for (int i = 0; i < matches.length; i++) {
            FileMatch match = matches[i];
            int start = Math.max(match.getOriginalOffset() - lineElement.getOffset(), 0);
            // append gap between last match and the new one
            if (pos < start) {
                if (charsToCut > 0) {
                    charsToCut = appendShortenedGap(content, pos, start, charsToCut, i == 0, str);
                } else {
                    str += content.substring(pos, start);
                }
            }
            // append match
            int end = Math.min(match.getOriginalOffset() + match.getOriginalLength() - lineElement.getOffset(),
                    lineElement.getLength());
            str += content.substring(start, end);
            pos = end;
        }
        // append rest of the line
        if (charsToCut > 0) {
            appendShortenedGap(content, pos, length, charsToCut, false, str);
        } else {
            str += content.substring(pos);
        }
        return str;
    }

    private static final int MIN_MATCH_CONTEXT = 10; // minimal number of characters shown after and before a match

    private int appendShortenedGap(String content, int start, int end, int charsToCut, boolean isFirst, String str) {
        int gapLength = end - start;
        if (!isFirst) {
            gapLength -= MIN_MATCH_CONTEXT;
        }
        if (end < content.length()) {
            gapLength -= MIN_MATCH_CONTEXT;
        }
        if (gapLength < MIN_MATCH_CONTEXT) { // don't cut, gap is too small
            str += content.substring(start, end);
            return charsToCut;
        }

        int context = MIN_MATCH_CONTEXT;
        if (gapLength > charsToCut) {
            context += gapLength - charsToCut;
        }

        if (!isFirst) {
            str += content.substring(start, start + context); // give all extra context to the right side of a match
            context = MIN_MATCH_CONTEXT;
        }

        str += fgEllipses;

        if (end < content.length()) {
            str += content.substring(end - context, end);
        }
        return charsToCut - gapLength + fgEllipses.length();
    }

    private int getCharsToCut(int contentLength, Match[] matches) {
        if (contentLength <= 256 || !"win32".equals(SWT.getPlatform()) || matches.length == 0) { //$NON-NLS-1$
            return 0; // no shortening required
        }
        // XXX: workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=38519
        return contentLength - 256 + Math.max(matches.length * fgEllipses.length(), 100);
    }

    private int evaluateLineStart(Match[] matches, String lineContent, int lineOffset) {
        int max = lineContent.length();
        if (matches.length > 0) {
            FileMatch match = (FileMatch) matches[0];
            max = match.getOriginalOffset() - lineOffset;
            if (max < 0) {
                return 0;
            }
        }
        for (int i = 0; i < max; i++) {
            char ch = lineContent.charAt(i);
            if (!Character.isWhitespace(ch) || ch == '\n' || ch == '\r') {
                return i;
            }
        }
        return max;
    }

    private String getColoredLabelWithCounts(Object element, String coloredName) {
        AbstractTextSearchResult result = fPage.getInput();
        if (result == null) {
            return coloredName;
        }

        int matchCount = result.getMatchCount(element);
        if (matchCount <= 1) {
            return coloredName;
        }

        String countInfo = MessageFormat.format(SearchMessages.FileLabelProvider_count_format, new Integer(matchCount));
        coloredName += " ";
        coloredName += countInfo;
        return coloredName;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
     */
    @Override
    public Image getImage(Object element) {
        if (element instanceof LineElement) {
            return fLineMatchImage;
        }
        if (!(element instanceof IResource)) {
            return null;
        }

        IResource resource = (IResource) element;
        Image image = fLabelProvider.getImage(resource);
        return image;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        fLabelProvider.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.BaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
     */
    @Override
    public boolean isLabelProperty(Object element, String property) {
        return fLabelProvider.isLabelProperty(element, property);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.BaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    @Override
    public void removeListener(ILabelProviderListener listener) {
        super.removeListener(listener);
        fLabelProvider.removeListener(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.BaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    @Override
    public void addListener(ILabelProviderListener listener) {
        super.addListener(listener);
        fLabelProvider.addListener(listener);
    }

    //	private static String decorateColoredString(String string, String decorated, Styler color) {
    //		String label= string.toString();
    //		int originalStart= decorated.indexOf(label);
    //		if (originalStart == -1) {
    //			return new String(decorated); // the decorator did something wild
    //		}
    //		if (originalStart > 0) {
    //			String newString= new String(decorated.substring(0, originalStart), color);
    //			newString += string;
    //			string= newString;
    //		}
    //		if (decorated.length() > originalStart + label.length()) { // decorator appended something
    //			return string.append(decorated.substring(originalStart + label.length()), color);
    //		}
    //		return string; // no change
    //	}

}
