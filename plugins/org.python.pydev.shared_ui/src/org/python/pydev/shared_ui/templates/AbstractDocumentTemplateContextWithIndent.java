/******************************************************************************
* Copyright (C) 2007-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.templates;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;

public abstract class AbstractDocumentTemplateContextWithIndent extends DocumentTemplateContext {

    public final String indentTo;

    public AbstractDocumentTemplateContextWithIndent(TemplateContextType type, IDocument document, int offset,
            int length, String indentTo) {
        super(type, document, offset, length);
        this.indentTo = indentTo != null ? indentTo : "";
    }

    protected abstract int getTabWidth();

    protected abstract boolean getUseSpaces();

    @Override
    public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {

        if (!canEvaluate(template)) {
            return null;
        }

        String spacesIndentString = StringUtils.createSpaceString(getTabWidth());

        //indent to needed level and
        //replace any \t for the indentation string 
        String pattern = template.getPattern();
        List<String> splitted = StringUtils.splitInLines(pattern);

        boolean changed = false;
        if (getUseSpaces()) {
            if (pattern.indexOf("\t") != -1) {
                template = createNewTemplate(template,
                        StringUtils.replaceAll(pattern, "\t", spacesIndentString));
                changed = true;
            }
        } else {
            if (pattern.indexOf(spacesIndentString) != -1) {
                FastStringBuffer newPattern = new FastStringBuffer();
                FastStringBuffer newTabsIndent = new FastStringBuffer();

                for (int i = 0; i < splitted.size(); i++) {
                    String string = splitted.get(i);

                    int spacesFound = 0;
                    while (string.length() > 0 && string.charAt(0) == ' ') {
                        string = string.substring(1);
                        spacesFound += 1;
                    }

                    int tabsToAdd = 0;
                    if (spacesFound > 0) {
                        tabsToAdd = spacesFound / spacesIndentString.length();
                        if (spacesFound % spacesIndentString.length() != 0) {
                            tabsToAdd += 1;
                        }
                        newTabsIndent.clear();
                        for (int j = 0; j < tabsToAdd; j++) {
                            newTabsIndent.append("\t");
                        }
                        newPattern.append(newTabsIndent);
                    }
                    newPattern.append(string);
                }
                template = createNewTemplate(template, newPattern.toString());
                changed = true;
            }
        }

        //recreate it (if needed). 
        if (changed) {
            pattern = template.getPattern();
            splitted = StringUtils.splitInLines(pattern);
        }

        String indentToStr = indentTo;
        String endLineDelim = TextSelectionUtils.getDelimiter(this.getDocument());

        int size = splitted.size();
        if (size > 0) {

            FastStringBuffer buffer = new FastStringBuffer("", (pattern.length() + (size * 2))
                    + ((size + 1) * indentToStr.length()));
            for (int i = 0; i < size; i++) { //we don't want to get the first line

                if (i != 0) {
                    //the 1st line is not indented (that's where the user requested the completion -- others should be indented to it)
                    buffer.append(indentToStr);
                }

                String str = splitted.get(i);

                //we have to make the new line delimiter correct:
                //https://sourceforge.net/tracker/index.php?func=detail&aid=2019419&group_id=85796&atid=577329
                boolean hasNewLine = false;
                if (str.endsWith("\r") || str.endsWith("\n")) {
                    hasNewLine = true;
                    if (str.endsWith("\r\n")) {
                        str = str.substring(0, str.length() - 2);
                    } else {
                        str = str.substring(0, str.length() - 1);
                    }
                }
                buffer.append(str);
                if (hasNewLine) {
                    buffer.append(endLineDelim);
                }
            }
            //just to change the pattern...
            template = createNewTemplate(template, buffer.toString());
        }

        try {
            TemplateTranslator translator = new TemplateTranslator();
            TemplateBuffer templateBuffer = translator.translate(template);

            getContextType().resolve(templateBuffer, this);

            return templateBuffer;
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }
    }

    private Template createNewTemplate(Template template, String newString) {
        return new Template(template.getName(), template.getDescription(), template.getContextTypeId(), newString,
                template.isAutoInsertable());
    }

}
