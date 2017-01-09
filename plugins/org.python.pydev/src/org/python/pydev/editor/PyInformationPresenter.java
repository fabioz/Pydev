/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.tooltips.presenter.AbstractInformationPresenter;

/**
 * Based on HTMLTextPresenter
 * 
 * @author Fabio
 */
public class PyInformationPresenter extends AbstractInformationPresenter {

    public static class PyStyleRange extends StyleRange {
        public PyStyleRange() {
        }

        public PyStyleRange(int start, int length, Color foreground, Color background) {
            super(start, length, foreground, background);
        }

        public PyStyleRange(int start, int length, Color foreground, Color background, int fontStyle) {
            super(start, length, foreground, background, fontStyle);
        }

        public String tagReplaced;
    }

    private int fCounter;
    private boolean fEnforceUpperLineLimit;
    private ControlListener resizeCallback;

    public PyInformationPresenter(boolean enforceUpperLineLimit) {
        super();
        fEnforceUpperLineLimit = enforceUpperLineLimit;
    }

    public PyInformationPresenter() {
        this(true);
    }

    private ControlListener resizeListener = new ControlListener() {

        @Override
        public void controlMoved(ControlEvent e) {
            if (resizeCallback != null) {
                resizeCallback.controlMoved(e);
            }
        }

        @Override
        public void controlResized(ControlEvent e) {
            if (resizeCallback != null) {
                resizeCallback.controlResized(e);
            }
        }

    };

    /**
     * Creates the reader and properly puts the presentation into place.
     */
    public Reader createReader(String hoverInfo, TextPresentation presentation) {
        String str = PyStringUtils.removeWhitespaceColumnsToLeft(hoverInfo);

        str = correctLineDelimiters(str);

        List<PyStyleRange> lst = new ArrayList<>();

        str = handlePydevTags(lst, str);

        Collections.sort(lst, new Comparator<PyStyleRange>() {

            @Override
            public int compare(PyStyleRange o1, PyStyleRange o2) {
                return Integer.compare(o1.start, o2.start);
            }
        });

        for (PyStyleRange pyStyleRange : lst) {
            presentation.addStyleRange(pyStyleRange);
        }

        return new StringReader(str);
    }

    /**
     * Changes for bold any Pydev hints.
     */
    private String handlePydevTags(List<PyStyleRange> lst, String str) {
        FastStringBuffer buf = new FastStringBuffer(str.length());

        String newString = handleLinks(lst, str, buf.clear(), "pydev_hint_bold", false);
        newString = handleLinks(lst, newString, buf.clear(), "pydev_link", true);
        return newString;
    }

    private String handleLinks(List<PyStyleRange> lst, String str, FastStringBuffer buf, String tag,
            boolean addLinkUnderline) {
        int lastIndex = 0;

        String startTag = "<" + tag;

        String endTag = "</" + tag + ">";
        int endTagLen = endTag.length();

        while (true) {
            int start = str.indexOf(startTag, lastIndex);
            if (start == -1) {
                break;
            }
            int startTagLen = str.indexOf(">", start) - start + 1;
            int end = str.indexOf(endTag, start + startTagLen);
            if (end == -1 || end == start) {
                break;
            }
            int initialIndex = lastIndex;
            lastIndex = end + endTagLen;

            buf.append(str.substring(initialIndex, start));
            int startRange = buf.length();

            buf.append(str.substring(start + startTagLen, end));
            int endRange = buf.length();

            PyStyleRange styleRange = new PyStyleRange(startRange, endRange - startRange,
                    JFaceColors.getHyperlinkText(Display.getDefault()), null, SWT.BOLD);
            styleRange.tagReplaced = str.substring(start, start + startTagLen);
            if (addLinkUnderline) {
                styleRange.underline = true;
                try {
                    styleRange.underlineStyle = SWT.UNDERLINE_LINK;
                } catch (Throwable e) {
                    //Ignore (not available on earlier versions of eclipse)
                }
            }
            lst.add(styleRange);
        }

        buf.append(str.substring(lastIndex, str.length()));
        String newString = buf.toString();
        return newString;
    }

    @SuppressWarnings("unchecked")
    protected void adaptTextPresentation(TextPresentation presentation, int offset, int insertLength) {

        int yoursStart = offset;
        int yoursEnd = offset + insertLength - 1;
        yoursEnd = Math.max(yoursStart, yoursEnd);

        Iterator<StyleRange> e = presentation.getAllStyleRangeIterator();
        while (e.hasNext()) {

            StyleRange range = e.next();

            int myStart = range.start;
            int myEnd = range.start + range.length - 1;
            myEnd = Math.max(myStart, myEnd);

            if (myEnd < yoursStart) {
                continue;
            }

            if (myStart < yoursStart) {
                range.length += insertLength;
            } else {
                range.start += insertLength;
            }
        }
    }

    private void append(FastStringBuffer buffer, String string, TextPresentation presentation) {

        int length = string.length();
        buffer.append(string);

        if (presentation != null) {
            adaptTextPresentation(presentation, fCounter, length);
        }

        fCounter += length;
    }

    private String getIndent(String line) {
        int length = line.length();

        int i = 0;
        while (i < length && Character.isWhitespace(line.charAt(i))) {
            ++i;
        }

        return (i == length ? line : line.substring(0, i)) + " "; //$NON-NLS-1$
    }

    /*
     * @see IHoverInformationPresenterExtension#updatePresentation(Drawable drawable, String, TextPresentation, int, int)
     * @since 3.2
     */
    @Override
    public String updatePresentation(Drawable drawable, String hoverInfo, TextPresentation presentation, int maxWidth,
            int maxHeight) {
        if (drawable instanceof StyledText) {
            final StyledText styledText = (StyledText) drawable;
            styledText.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseDown(MouseEvent e) {
                    int offset;
                    try {
                        offset = styledText.getOffsetAtLocation(new Point(e.x, e.y));
                    } catch (IllegalArgumentException e1) {
                        return; //invalid location
                    }
                    StyleRange r = styledText.getStyleRangeAtOffset(offset);
                    if (r instanceof PyStyleRange) {
                        String tagReplaced = ((PyStyleRange) r).tagReplaced;
                        if (tagReplaced != null) {
                            String start = "<pydev_link pointer=\"";
                            String end = "\">";
                            if (tagReplaced.startsWith(start) && tagReplaced.endsWith(end)) {
                                String pointer = tagReplaced.substring(start.length(),
                                        tagReplaced.length() - end.length());
                                new PyOpenAction().run(ItemPointer.fromPortableString(pointer));
                            }
                        }
                    }
                }
            });
        }

        if (drawable instanceof Control) {
            Control control = (Control) drawable;
            if (!Arrays.asList(control.getListeners(SWT.Resize)).contains(resizeListener)) {
                control.addControlListener(resizeListener);
            }
        }

        if (hoverInfo == null) {
            return null;
        }

        GC gc = new GC(drawable);
        try {

            FastStringBuffer buffer = new FastStringBuffer();
            int maxNumberOfLines = Math.round((float) maxHeight / (float) gc.getFontMetrics().getHeight());

            fCounter = 0;
            PyLineBreakReader reader = new PyLineBreakReader(createReader(hoverInfo, presentation), gc, maxWidth);

            boolean lastLineFormatted = false;
            String lastLineIndent = null;

            String line = reader.readLine();
            boolean lineFormatted = reader.isFormattedLine();
            boolean firstLineProcessed = false;

            while (line != null) {

                if (fEnforceUpperLineLimit && maxNumberOfLines <= 0) {
                    break;
                }

                if (firstLineProcessed) {
                    if (!lastLineFormatted) {
                        append(buffer, LINE_DELIM, null);
                    } else {
                        append(buffer, LINE_DELIM, presentation);
                        if (lastLineIndent != null) {
                            append(buffer, lastLineIndent, presentation);
                        }
                    }
                }

                append(buffer, line, null);
                firstLineProcessed = true;

                lastLineFormatted = lineFormatted;
                if (!lineFormatted) {
                    lastLineIndent = null;
                } else if (lastLineIndent == null) {
                    lastLineIndent = getIndent(line);
                }

                line = reader.readLine();
                lineFormatted = reader.isFormattedLine();

                maxNumberOfLines--;
            }

            if (line != null) {
                append(buffer, LINE_DELIM, lineFormatted ? presentation : null);
            }

            return trim(buffer, presentation);

        } catch (IOException e) {

            // ignore TODO do something else?
            return null;

        } finally {
            gc.dispose();
        }
    }

    private String trim(FastStringBuffer buffer, TextPresentation presentation) {

        int length = buffer.length();

        int end = length - 1;
        while (end >= 0 && Character.isWhitespace(buffer.charAt(end))) {
            --end;
        }

        if (end == -1) {
            return ""; //$NON-NLS-1$
        }

        if (end < length - 1) {
            buffer.delete(end + 1, length);
        } else {
            end = length;
        }

        int start = 0;
        while (start < end && Character.isWhitespace(buffer.charAt(start))) {
            ++start;
        }

        buffer.delete(0, start);
        presentation.setResultWindow(new Region(start, buffer.length()));
        return buffer.toString();
    }

    /**
     * Add a listener to be notified when the hover control is resized.
     * @param listener the callback listener
     */
    public void addResizeCallback(ControlListener listener) {
        this.resizeCallback = listener;
    }
}
