package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.IAssistProps;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.partitioner.FastPartitioner;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class AssistFString implements IAssistProps {
    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature, IPyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        PySourceViewer viewer = null;
        if (edit instanceof PyEdit) { //only in tests it is actually null
            viewer = ((PyEdit) edit).getPySourceViewer();
        }
        List<ICompletionProposalHandle> lst = new ArrayList<ICompletionProposalHandle>();

        IDocument doc = ps.getDoc();
        ITypedRegion partition = ((FastPartitioner) PyPartitionScanner.checkPartitionScanner(doc)).getPartition(offset);
        if (!ParsingUtils.isStringContentType(partition.getType())) {
            return lst;
        }

        int formatCount = StringUtils.count(doc.get(partition.getOffset(), partition.getLength()), "%s");
        if (formatCount == 0) {
            return lst;
        }

        // get string end offset
        int partitionEndOffset = partition.getOffset() + partition.getLength();
        // get format end offset
        int i = partitionEndOffset;
        try {
            ParsingUtils parsingUtils = ParsingUtils.create(doc);
            while (i < parsingUtils.len()) {
                char c = parsingUtils.charAt(i);
                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }
                if (c == '{' || c == '[' || c == '(' || c == '"' || c == '\'') {
                    i = parsingUtils.eatPar(i, null, c) + 1;
                    break;
                }
                if (Character.isJavaIdentifierPart(c)) {
                    i++;
                    while (i < parsingUtils.len()) {
                        c = parsingUtils.charAt(i);
                        if (Character.isJavaIdentifierPart(c) || c == '.') {
                            i++;
                            continue;
                        } else if (c == '{' || c == '[' || c == '(' || c == '"' || c == '\'') {
                            i = parsingUtils.eatPar(i, null, c) + 1;
                            c = parsingUtils.charAt(i);
                            while (Character.isWhitespace(c)) {
                                i++;
                                c = parsingUtils.charAt(i);
                            }
                            if (c == '.') {
                                i++;
                                continue;
                            }
                            break; // inner break
                        }
                    }
                    break; // outer break
                }
                i++;
            }

            FastStringBuffer variablesBuf = new FastStringBuffer();
            variablesBuf.append(doc.get(partitionEndOffset, i - partitionEndOffset));
            // get only the variable names and remove the last comma
            variablesBuf.trim().deleteFirst();
            if (variablesBuf.trim().startsWith('(')) {
                variablesBuf.deleteFirst();
                variablesBuf.deleteLast();
                variablesBuf.trim();
            }
            if (variablesBuf.endsWith(',')) {
                variablesBuf.deleteLast();
                variablesBuf.trim();
            }

            // initialize format output string with the properly size allocation (f + string len + variables len)
            FastStringBuffer strBuf = new FastStringBuffer(1 + partition.getLength() + variablesBuf.length());
            strBuf.append('f').append(doc.get(partition.getOffset(), partition.getLength()));

            // get variables without literals
            String variablesWithoutLiterals = PySelection.getLineWithoutCommentsOrLiterals(variablesBuf.toString());
            // get variable by variable
            List<String> variables = new ArrayList<String>();
            parsingUtils = ParsingUtils.create(variablesBuf);
            int j = 0;
            while (j < parsingUtils.len()) {
                char c = parsingUtils.charAt(j);
                if (Character.isWhitespace(c) || c == ',') {
                    j++;
                    continue;
                }
                int commaPos;
                if (c == '{' || c == '[' || c == '(' || c == '"' || c == '\'') {
                    commaPos = parsingUtils.eatPar(j, null, c) + 1;
                } else {
                    commaPos = variablesWithoutLiterals.indexOf(',', j);
                }
                if (commaPos != -1) {
                    CharSequence subSequence = variablesBuf.subSequence(j, commaPos);
                    variables.add(subSequence.toString().trim());
                    j = commaPos + 1;
                } else {
                    CharSequence subSequence = variablesBuf.subSequence(j, variablesBuf.length());
                    variables.add(subSequence.toString().trim());
                    break;
                }
            }
            // iterate through variables and edit the f-string output
            if (formatCount == variables.size()) {
                for (String variable : variables) {
                    strBuf.replaceFirst("%s", "{" + variable + "}");
                }
            } else if (variables.size() == 1) {
                String variable = variables.get(0);
                for (j = 0; j < formatCount; j++) {
                    strBuf.replaceFirst("%s", "{" + variable + "[" + j + "]" + "}");
                }
            } else {
                return lst;
            }
            lst.add(CompletionProposalFactory.get().createPyCompletionProposal(strBuf.toString(),
                    partition.getOffset(), i - partition.getOffset(), 0, getImage(imageCache,
                            UIConstants.COMPLETION_TEMPLATE),
                    "Convert to f-string", null, null, IPyCompletionProposal.PRIORITY_DEFAULT, null));
        } catch (SyntaxErrorException e) {
            Log.log(e);
        }

        return lst;
    }

    private IImageHandle getImage(IImageCache imageCache, String c) {
        if (imageCache != null) {
            return imageCache.get(c);
        }
        return null;
    }

    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        return sel.indexOf("%s") != -1;
    }
}
