/*
 * Created on Jul 1, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Based on JavaParameterListValidator
 * 
 * @author Fabio
 */
public class PyContextInformationValidator implements IContextInformationValidator, IContextInformationPresenter {

    public PyCalltipsContextInformation fInformation;

    public IDocument doc;

    public int fCurrentParameter;

    public boolean returnedFalseOnce;

    private int fPosition;

    /**
     * Holds the comma positions for the context information to display
     */
    int[] commaPositions;

    /**
     * IContextInformationValidator
     */
    public void install(IContextInformation info, IDocument doc, int offset) {
        this.returnedFalseOnce = false;
        this.fInformation = (PyCalltipsContextInformation) info;
        this.doc = doc;
        this.fPosition = fInformation.getReplacementOffset();
        this.fCurrentParameter = -1;
        this.commaPositions = null;
    }

    /**
     * IContextInformationPresenter
     */
    public void install(IContextInformation info, ITextViewer viewer, int offset) {
        install(info, viewer.getDocument(), offset);
    }

    /**
     * @see IContextInformationValidator#isContextInformationValid(int)
     */
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
            PydevPlugin.log(x);
            return false;
        }
    }

    //--- interface from IContextInformationPresenter

    /**
     * @see IContextInformationPresenter#updatePresentation(int, TextPresentation)
     */
    public boolean updatePresentation(int position, TextPresentation presentation) {
        int currentParameter = -1;

        try {
            currentParameter = getCurrentParameter(doc, fPosition, position, ",", "", true);
        } catch (BadLocationException x) {
            return false;
        }

        if (fCurrentParameter != -1) {
            if (currentParameter == fCurrentParameter) {
                return false;
            }
        }

        presentation.clear();
        fCurrentParameter = currentParameter;

        String s = fInformation.getInformationDisplayString();
        int[] commas = computeCommaPositions();

        if (commas.length - 2 < fCurrentParameter) {
            presentation.addStyleRange(new StyleRange(0, s.length(), null, null, SWT.NORMAL));
            return true;
        }

        int start = commas[fCurrentParameter] + 1;
        int end = commas[fCurrentParameter + 1];
        if (start > 0) {
            presentation.addStyleRange(new StyleRange(0, start, null, null, SWT.NORMAL));
        }

        if (end > start) {
            presentation.addStyleRange(new StyleRange(start, end - start, null, null, SWT.BOLD));
        }

        if (end < s.length()) {
            presentation.addStyleRange(new StyleRange(end, s.length() - end, null, null, SWT.NORMAL));
        }

        return true;
    }

    private int[] computeCommaPositions() {
        if (commaPositions == null) {
            final String code = fInformation.getInformationDisplayString();
            final int length = code.length();
            int pos = 0;
            List<Integer> positions = new ArrayList<Integer>();
            positions.add(new Integer(-1));
            while (pos < length && pos != -1) {
                char ch = code.charAt(pos);
                switch (ch) {
                case ',':
                    positions.add(new Integer(pos));
                    break;
                default:
                    break;
                }
                if (pos != -1)
                    pos++;
            }
            positions.add(new Integer(length));

            int[] fields = new int[positions.size()];
            for (int i = 0; i < fields.length; i++) {
                fields[i] = ((Integer) positions.get(i)).intValue();
            }
            commaPositions = fields;
        }
        return commaPositions;
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
     */
    public int getCurrentParameter(IDocument document, final int start, final int end, String increments,
            String decrements, boolean considerNesting) throws BadLocationException {

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
                int eaten = ParsingUtils.eatLiterals(document, new StringBuffer(), offset - 1) + 1;
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
