/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.FontUtils;
import org.python.pydev.shared_ui.IFontUsage;
import org.python.pydev.ui.ColorAndStyleCache;

/**
 * This class can create a styled text and later format a python code string and give style ranges for
 * that string so that it's properly highlighted with the colors in the passed preferences.  
 */
public class StyledTextForShowingCodeFactory implements IPropertyChangeListener {

    /**
     * The styled text returned.
     */
    private StyledText styledText;

    /**
     * Used to hold the background color (it cannot be disposed while we're using it).
     */
    private ColorAndStyleCache backgroundColorCache;

    /**
     * Used to hold other colors (always cleared when new preferences are set).
     */
    private ColorAndStyleCache colorCache;

    /**
     * @return a styled text that can be used to show code with the colors based on the color cache received.
     */
    public StyledText createStyledTextForCodePresentation(Composite parent) {
        styledText = new StyledText(parent, SWT.BORDER | SWT.READ_ONLY);
        this.backgroundColorCache = new ColorAndStyleCache(new PreferenceStore());
        this.colorCache = new ColorAndStyleCache(null);

        try {
            styledText.setFont(new Font(parent.getDisplay(), FontUtils.getFontData(IFontUsage.STYLED, true)));
        } catch (Throwable e) {
            //ignore
        }
        updateBackgroundColor();

        PydevPrefs.getChainedPrefStore().addPropertyChangeListener(this);

        return styledText;
    }

    /**
     * Updates the color of the background.
     */
    private void updateBackgroundColor() {
        IPreferenceStore chainedPrefStore = PydevPrefs.getChainedPrefStore();

        Color backgroundColor = null;
        if (!chainedPrefStore.getBoolean(PyEdit.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)) {
            RGB backgroundRGB = PreferenceConverter.getColor(chainedPrefStore, PyEdit.PREFERENCE_COLOR_BACKGROUND);
            backgroundColor = backgroundColorCache.getColor(backgroundRGB);
        }
        styledText.setBackground(backgroundColor);
    }

    /**
     * When the background changes, we need to update the background color (for the next refresh).
     */
    public void propertyChange(PropertyChangeEvent event) {
        String prop = event.getProperty();
        if (PyEdit.PREFERENCE_COLOR_BACKGROUND.equals(prop)
                || PyEdit.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT.equals(prop)) {
            updateBackgroundColor();
        }
        ;
    }

    /**
     * It needs to be called so that we're properly garbage-collected and clear our caches.
     */
    public void dispose() {
        PydevPrefs.getChainedPrefStore().removePropertyChangeListener(this);
        this.backgroundColorCache.dispose();
        this.colorCache.dispose();
    }

    /**
     * This method will format the passed string with the passed standard and create style ranges for the returned
     * string, so that the code is properly seen by the user in a StyledText.
     * 
     * @param formatStd the coding standard that should be used for the parse.
     * @param str the string that should be formatted and have the colors applied.
     * @param prefs the preferences that contain the colors to be used for each partition.
     * @param showSpacesAndNewLines if true, spaces will be shown as dots and new lines shown as a '\n' string
     *        (otherwise they're not visible).
     */
    @SuppressWarnings("unchecked")
    public Tuple<String, StyleRange[]> formatAndGetStyleRanges(FormatStd formatStd, String str, IPreferenceStore prefs,
            boolean showSpacesAndNewLines) {
        //When new preferences are set, the cache is reset (the background color doesn't need to be 
        //cleared because the colors are gotten from the rgb and not from the names).
        this.colorCache.setPreferences(prefs);

        PyFormatStd formatter = new PyFormatStd();
        try {
            Document doc = new Document(str);
            formatter.formatAll(doc, null, false, formatStd, false);
            str = doc.get();
        } catch (SyntaxErrorException e) {
        }
        FastStringBuffer buf = new FastStringBuffer();
        for (String line : StringUtils.splitInLines(str)) {
            buf.append(line);
            char c = buf.lastChar();
            if (c == '\n') {
                buf.deleteLast();
                if (showSpacesAndNewLines) {
                    buf.append("\\n");
                }
                //Adds chars so that the initial presentation is bigger (they are later changed for spaces).
                //If that's not done, the created editor would be too small... especially if we consider
                //that the code-formatting can change for that editor (so, some parts wouldn't appear if we
                //need more space later on).
                buf.appendN('|', 8);
                buf.append(c);
            }
        }
        String result = buf.toString();

        String finalResult;
        if (showSpacesAndNewLines) {
            finalResult = result.replace(' ', '.');
        } else {
            finalResult = result;
        }
        finalResult = finalResult.replace('|', ' ');

        PyPartitionScanner pyPartitionScanner = new PyPartitionScanner();
        FastPartitioner fastPartitioner = new FastPartitioner(pyPartitionScanner, IPythonPartitions.types);
        Document doc = new Document(result);
        fastPartitioner.connect(doc);

        TextPresentation textPresentation = new TextPresentation();
        PyCodeScanner scanner = new PyCodeScanner(colorCache);
        try {
            ITypedRegion[] computePartitioning = fastPartitioner.computePartitioning(0, doc.getLength());
            for (ITypedRegion region : computePartitioning) {
                String type = region.getType();
                int offset = region.getOffset();
                int len = region.getLength();
                if (IPythonPartitions.PY_DEFAULT.equals(type) || type == null) {
                    createDefaultRanges(textPresentation, scanner, doc, offset, len);

                } else if (IPythonPartitions.PY_COMMENT.equals(type)) {
                    TextAttribute textAttribute = colorCache.getCommentTextAttribute();
                    textPresentation.addStyleRange(new StyleRange(offset, len, textAttribute.getForeground(), null,
                            textAttribute.getStyle()));

                } else if (IPythonPartitions.PY_BACKQUOTES.equals(type)) {
                    TextAttribute textAttribute = colorCache.getBackquotesTextAttribute();
                    textPresentation.addStyleRange(new StyleRange(offset, len, textAttribute.getForeground(), null,
                            textAttribute.getStyle()));

                } else if (IPythonPartitions.PY_MULTILINE_BYTES1.equals(type)
                        || IPythonPartitions.PY_MULTILINE_BYTES2.equals(type)
                        || IPythonPartitions.PY_SINGLELINE_BYTES1.equals(type)
                        || IPythonPartitions.PY_SINGLELINE_BYTES2.equals(type)) {
                    TextAttribute textAttribute = colorCache.getStringTextAttribute();
                    textPresentation.addStyleRange(new StyleRange(offset, len, textAttribute.getForeground(), null,
                            textAttribute.getStyle()));

                } else if (IPythonPartitions.PY_MULTILINE_UNICODE1.equals(type)
                        || IPythonPartitions.PY_MULTILINE_UNICODE2.equals(type)
                        || IPythonPartitions.PY_SINGLELINE_UNICODE1.equals(type)
                        || IPythonPartitions.PY_SINGLELINE_UNICODE2.equals(type)) {
                    TextAttribute textAttribute = colorCache.getUnicodeTextAttribute();
                    textPresentation.addStyleRange(new StyleRange(offset, len, textAttribute.getForeground(), null,
                            textAttribute.getStyle()));

                } else if (IPythonPartitions.PY_MULTILINE_BYTES_OR_UNICODE1.equals(type)
                        || IPythonPartitions.PY_MULTILINE_BYTES_OR_UNICODE2.equals(type)
                        || IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE1.equals(type)
                        || IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE2.equals(type)) {
                    //In this case, although we have a choice, make it similar to unicode.
                    TextAttribute textAttribute = colorCache.getUnicodeTextAttribute();
                    textPresentation.addStyleRange(new StyleRange(offset, len, textAttribute.getForeground(), null,
                            textAttribute.getStyle()));
                }
            }
        } finally {
            fastPartitioner.disconnect();
        }

        if (showSpacesAndNewLines) {
            for (int i = 0; i < result.length(); i++) {
                char curr = result.charAt(i);
                if (curr == '\\' && i + 1 < result.length() && result.charAt(i + 1) == 'n') {
                    textPresentation.mergeStyleRange(new StyleRange(i, 2, colorCache.getColor(new RGB(180, 180, 180)),
                            null));
                    i += 1;
                } else if (curr == ' ') {
                    int finalI = i;
                    for (; finalI < result.length() && result.charAt(finalI) == ' '; finalI++) {
                        //just iterate (the finalI will have the right value at the end).
                    }
                    textPresentation.mergeStyleRange(new StyleRange(i, finalI - i, colorCache.getColor(new RGB(180,
                            180, 180)), null));

                }
            }
        }

        ArrayList<StyleRange> list = new ArrayList<StyleRange>();
        Iterator<StyleRange> it = textPresentation.getAllStyleRangeIterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        StyleRange[] ranges = list.toArray(new StyleRange[list.size()]);
        return new Tuple<String, StyleRange[]>(finalResult, ranges);
    }

    /**
     * Creates the ranges from parsing the code with the PyCodeScanner.
     * 
     * @param textPresentation this is the container of the style ranges.
     * @param scanner the scanner used to parse the document.
     * @param doc document to parse.
     * @param partitionOffset the offset of the document we should parse.
     * @param partitionLen the length to be parsed.
     */
    private void createDefaultRanges(TextPresentation textPresentation, PyCodeScanner scanner, Document doc,
            int partitionOffset, int partitionLen) {

        scanner.setRange(doc, partitionOffset, partitionLen);

        IToken nextToken = scanner.nextToken();
        while (!nextToken.isEOF()) {
            Object data = nextToken.getData();
            if (data instanceof TextAttribute) {
                TextAttribute textAttribute = (TextAttribute) data;
                int offset = scanner.getTokenOffset();
                int len = scanner.getTokenLength();
                Color foreground = textAttribute.getForeground();
                Color background = textAttribute.getBackground();
                int style = textAttribute.getStyle();
                textPresentation.addStyleRange(new StyleRange(offset, len, foreground, background, style));

            }
            nextToken = scanner.nextToken();
        }
    }

}
