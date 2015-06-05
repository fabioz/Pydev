/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Juerg Billeter, juergbi@ethz.ch - 47136 Search view should show match objects
 *     Ulrich Etter, etteru@ethz.ch - 47136 Search view should show match objects
 *     Roman Fuchs, fuchsro@ethz.ch - 47136 Search view should show match objects
 *******************************************************************************/
package com.python.pydev.analysis.search_index;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.search.internal.ui.SearchPluginImages;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;

import com.python.pydev.analysis.search.ICustomLineElement;
import com.python.pydev.analysis.search.ICustomMatch;

/**
 * Copy from org.eclipse.search.internal.ui.text.FileLabelProvider
 */
public class SearchIndexLabelProvider extends LabelProvider implements IStyledLabelProvider {

    public static final int SHOW_LABEL = 1;
    public static final int SHOW_LABEL_PATH = 2;
    public static final int SHOW_PATH_LABEL = 3;

    private static final String fgSeparatorFormat = "{0} - {1}"; //$NON-NLS-1$

    private static final String fgEllipses = " ... "; //$NON-NLS-1$

    private final WorkbenchLabelProvider fLabelProvider;
    private final AbstractTextSearchViewPage fPage;
    private final Comparator fMatchComparator;

    private final Image fLineMatchImage;

    private int fOrder;

    public SearchIndexLabelProvider(AbstractTextSearchViewPage page, int orderFlag) {
        fLabelProvider = new WorkbenchLabelProvider();
        fOrder = orderFlag;
        fPage = page;
        fLineMatchImage = SearchPluginImages.get(SearchPluginImages.IMG_OBJ_TEXT_SEARCH_LINE);
        fMatchComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((ICustomMatch) o1).getOriginalOffset() - ((ICustomMatch) o2).getOriginalOffset();
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
        return getStyledText(object).getString();
    }

    @Override
    public StyledString getStyledText(Object element) {
        if (element instanceof TreeNode) {
            element = ((TreeNode<?>) element).data;
        }

        if (element instanceof ICustomLineElement) {
            return getLineElementLabel((ICustomLineElement) element);
        }

        if (!(element instanceof IResource)) {
            IResource resource = null;
            if (element instanceof IAdaptable) {
                IAdaptable iAdaptable = (IAdaptable) element;
                resource = iAdaptable.getAdapter(IResource.class);
                if (resource != null) {
                    element = resource;
                }
            }
            if (!(element instanceof IResource)) {
                return new StyledString(element.toString());
            }
        }

        IResource resource = (IResource) element;
        if (!resource.exists()) {
            new StyledString("<removed resource>");
        }

        String name = BasicElementLabels.getResourceName(resource);
        if (fOrder == SHOW_LABEL) {
            return getColoredLabelWithCounts(resource, new StyledString(name));
        }

        String pathString = BasicElementLabels.getPathLabel(resource.getParent().getFullPath(), false);
        if (fOrder == SHOW_LABEL_PATH) {
            StyledString str = new StyledString(name);
            String decorated = MessageFormat.format(fgSeparatorFormat, str.getString(), pathString);

            StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER, str);
            return getColoredLabelWithCounts(resource, str);
        }

        StyledString str = new StyledString(MessageFormat.format(fgSeparatorFormat, pathString, name));
        return getColoredLabelWithCounts(resource, str);
    }

    private StyledString getLineElementLabel(ICustomLineElement lineElement) {
        int lineNumber = lineElement.getLine();
        String lineNumberString = MessageFormat.format("{0}:",
                new Integer(lineNumber));

        StyledString str = new StyledString(lineNumberString, StyledString.QUALIFIER_STYLER);

        Match[] matches = lineElement.getMatches(fPage.getInput());
        Arrays.sort(matches, fMatchComparator);

        String content = lineElement.getContents();

        int pos = evaluateLineStart(matches, content, lineElement.getOffset());

        int length = content.length();

        int charsToCut = getCharsToCut(length, matches); // number of characters to leave away if the line is too long
        for (int i = 0; i < matches.length; i++) {
            ICustomMatch match = (ICustomMatch) matches[i];
            int start = Math.max(match.getOriginalOffset() - lineElement.getOffset(), 0);
            // append gap between last match and the new one
            if (pos < start) {
                if (charsToCut > 0) {
                    charsToCut = appendShortenedGap(content, pos, start, charsToCut, i == 0, str);
                } else {
                    str.append(content.substring(pos, start));
                }
            }
            // append match
            int end = Math.min(match.getOriginalOffset() + match.getOriginalLength() - lineElement.getOffset(),
                    lineElement.getLength());
            str.append(content.substring(start, end), DecoratingFileSearchLabelProvider.HIGHLIGHT_STYLE);
            pos = end;
        }
        // append rest of the line
        if (charsToCut > 0) {
            appendShortenedGap(content, pos, length, charsToCut, false, str);
        } else {
            str.append(content.substring(pos));
        }
        return str;
    }

    private static final int MIN_MATCH_CONTEXT = 10; // minimal number of characters shown after and before a match

    private int appendShortenedGap(String content, int start, int end, int charsToCut, boolean isFirst,
            StyledString str) {
        int gapLength = end - start;
        if (!isFirst) {
            gapLength -= MIN_MATCH_CONTEXT;
        }
        if (end < content.length()) {
            gapLength -= MIN_MATCH_CONTEXT;
        }
        if (gapLength < MIN_MATCH_CONTEXT) { // don't cut, gap is too small
            str.append(content.substring(start, end));
            return charsToCut;
        }

        int context = MIN_MATCH_CONTEXT;
        if (gapLength > charsToCut) {
            context += gapLength - charsToCut;
        }

        if (!isFirst) {
            str.append(content.substring(start, start + context)); // give all extra context to the right side of a match
            context = MIN_MATCH_CONTEXT;
        }

        str.append(fgEllipses, StyledString.QUALIFIER_STYLER);

        if (end < content.length()) {
            str.append(content.substring(end - context, end));
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
            ICustomMatch match = (ICustomMatch) matches[0];
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

    private StyledString getColoredLabelWithCounts(Object element, StyledString coloredName) {
        AbstractTextSearchResult result = fPage.getInput();
        if (result == null) {
            return coloredName;
        }

        int matchCount = result.getMatchCount(element);
        if (matchCount <= 1) {
            return coloredName;
        }

        String countInfo = MessageFormat.format("({0} matches)", new Integer(matchCount));
        coloredName.append(' ').append(countInfo, StyledString.COUNTER_STYLER);
        return coloredName;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
     */
    @Override
    public Image getImage(Object element) {
        if (element instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) element;
            element = treeNode.data;
        }
        if (element instanceof ICustomLineElement) {
            return fLineMatchImage;
        }
        if (element instanceof CustomModule) {
            return SharedUiPlugin.getImageCache().get(UIConstants.PY_FILE_ICON);
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

}
