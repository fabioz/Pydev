/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 1, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;


/**
 * Based on JavaParameterListValidator
 * 
 * @author Fabio
 */
public class PyContextInformationValidator implements IContextInformationValidator, IContextInformationPresenter {

    public IPyCalltipsContextInformation fInformation;

    public IDocument doc;

    public boolean returnedFalseOnce;

    private int fPosition;

    /**
     * IContextInformationValidator
     */
    public void install(IContextInformation info, IDocument doc, int offset) {
        this.returnedFalseOnce = false;
        this.fInformation = (IPyCalltipsContextInformation) info;
        this.doc = doc;
        this.fPosition = fInformation.getShowCalltipsOffset();
    }

    /**
     * IContextInformationPresenter
     */
    @Override
    public void install(IContextInformation info, ITextViewer viewer, int offset) {
        install(info, viewer.getDocument(), offset);
    }

    /**
     * @see IContextInformationValidator#isContextInformationValid(int)
     */
    @Override
    public boolean isContextInformationValid(int position) {
        if (doc == null) {
            this.returnedFalseOnce = true;
            return false;
        }

        try {
            if (position < fPosition) {
                this.returnedFalseOnce = true;
                return false;
            }

            IDocument document = doc;
            IRegion line = document.getLineInformationOfOffset(fPosition);

            if (position < line.getOffset() || position >= document.getLength()) {
                this.returnedFalseOnce = true;
                return false;
            }

            boolean ret = getCurrentParameter(document, fPosition, position, "(", ")", false) >= 0; //$NON-NLS-1$ //$NON-NLS-2$
            if (ret == false) {
                returnedFalseOnce = true;
            }
            return ret;

        } catch (BadLocationException x) {
            this.returnedFalseOnce = true;
            return false;
        } catch (Exception x) {
            this.returnedFalseOnce = true;
            Log.log(x);
            return false;
        }
    }

    //--- interface from IContextInformationPresenter

    /**
     * @see IContextInformationPresenter#updatePresentation(int, TextPresentation)
     */
    @Override
    public boolean updatePresentation(int position, TextPresentation presentation) {
        return false;
    }

    /**
     * 
     * @param document the document from where the contents should be gotten.
     * @param start
     * @param end
     * @param increments this is the string that when found will increment the current parameter
     * @param decrements this is the string that when found will decrement the current parameter
     * @param considerNesting
     * @return
     * @throws BadLocationException
     * @throws SyntaxErrorException 
     */
    public int getCurrentParameter(IDocument document, final int start, final int end, String increments,
            String decrements, boolean considerNesting) throws BadLocationException, SyntaxErrorException {

        Assert.isTrue((increments.length() != 0 || decrements.length() != 0) && !increments.equals(decrements));

        final int NONE = 0;
        final int BRACKET = 1;
        final int BRACE = 2;
        final int PAREN = 3;
        final int ANGLE = 4;

        int nestingMode = NONE;
        int nestingLevel = 0;

        int charCount = 0;
        int offset = start;
        ParsingUtils parsingUtils = ParsingUtils.create(document);
        while (offset < end) {
            char curr = document.getChar(offset++);
            switch (curr) {
                case '#':
                    if (offset < end) {
                        // '#' comment: nothing to do anymore on this line
                        offset = end;
                    }
                    break;
                case '"':
                case '\'':
                    int eaten = parsingUtils.eatLiterals(null, offset - 1) + 1;
                    if (eaten > offset) {
                        offset = eaten;
                    }
                    break;
                case '[':
                    if (considerNesting) {
                        if (nestingMode == BRACKET || nestingMode == NONE) {
                            nestingMode = BRACKET;
                            nestingLevel++;
                        }
                        break;
                    }
                case ']':
                    if (considerNesting) {
                        if (nestingMode == BRACKET)
                            if (--nestingLevel == 0)
                                nestingMode = NONE;
                        break;
                    }
                case '(':
                    if (considerNesting) {
                        if (nestingMode == ANGLE) {
                            // generics heuristic failed
                            nestingMode = PAREN;
                            nestingLevel = 1;
                        }
                        if (nestingMode == PAREN || nestingMode == NONE) {
                            nestingMode = PAREN;
                            nestingLevel++;
                        }
                        break;
                    }
                case ')':
                    if (considerNesting) {
                        if (nestingMode == PAREN)
                            if (--nestingLevel == 0)
                                nestingMode = NONE;
                        break;
                    }
                case '{':
                    if (considerNesting) {
                        if (nestingMode == ANGLE) {
                            // generics heuristic failed
                            nestingMode = BRACE;
                            nestingLevel = 1;
                        }
                        if (nestingMode == BRACE || nestingMode == NONE) {
                            nestingMode = BRACE;
                            nestingLevel++;
                        }
                        break;
                    }
                case '}':
                    if (considerNesting) {
                        if (nestingMode == BRACE)
                            if (--nestingLevel == 0)
                                nestingMode = NONE;
                        break;
                    }

                default:
                    if (nestingLevel != 0)
                        continue;

                    if (increments.indexOf(curr) >= 0) {
                        ++charCount;
                    }

                    if (decrements.indexOf(curr) >= 0) {
                        --charCount;
                    }
            }
        }

        return charCount;
    }

}
