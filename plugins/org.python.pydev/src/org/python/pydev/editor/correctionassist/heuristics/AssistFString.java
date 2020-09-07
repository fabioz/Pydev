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
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.core.proposals.CompletionProposalFactory;
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
        List<ICompletionProposalHandle> lst = new ArrayList<ICompletionProposalHandle>();

        IDocument doc = ps.getDoc();
        ITypedRegion partition = ((FastPartitioner) PyPartitionScanner.checkPartitionScanner(doc)).getPartition(offset);
        if (!ParsingUtils.isStringContentType(partition.getType())) {
            return lst;
        }

        // check if the string prefix is byte literal or if it is already a f-string
        int partitionOffset = partition.getOffset();
        char firstPrefix = doc.getChar(partitionOffset);
        char penultPrefix = ' ';
        if (firstPrefix != '\'' && firstPrefix == '\"') {
            penultPrefix = doc.getChar(partitionOffset + 1);
        }
        if (firstPrefix == 'b' || penultPrefix == 'b' || firstPrefix == 'f' || penultPrefix == 'f') {
            return lst;
        }

        final String stringPartitionContents = doc.get(partitionOffset, partition.getLength());
        int formatCount = 0;
        // count how many format variables the string have
        formatCount += StringUtils.count(stringPartitionContents, "%s");
        formatCount += StringUtils.count(stringPartitionContents, "%r");

        ParsingUtils parsingUtils = ParsingUtils.create(doc);
        // get string end offset
        int partitionEndOffset = partitionOffset + partition.getLength();
        // we have to check if this is really a valid format statement
        // first of all, let's iterate to search for a %
        int i = partitionEndOffset;
        boolean validStmt = false;
        while (i < parsingUtils.len()) {
            char c = parsingUtils.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '%') {
                if (formatCount == 0) {
                    // if we do not have any format variables in string,
                    // there is no reason to search for variables to format
                    return lst;
                }
                validStmt = true;
                i++;
            }
            break;
        }
        if (validStmt) {
            try {
                // get format end offset and capture all the variables to format
                int formatVariablesLen = -1;
                boolean acceptMultiLines = false;
                List<String> variables = new ArrayList<String>();
                while (i < parsingUtils.len()) {
                    char c = parsingUtils.charAt(i);
                    if (Character.isWhitespace(c)) {
                        i++;
                        continue;
                    }
                    if (c == '{' || c == '%') {
                        return lst;
                    }
                    if (c == '[' || c == '(') {
                        formatVariablesLen = parsingUtils.eatPar(i, null, c) - partitionEndOffset + 1;
                        if (partitionEndOffset + formatVariablesLen > parsingUtils.len()) {
                            return lst;
                        }
                        i++;
                        c = parsingUtils.charAt(i);
                        acceptMultiLines = true;
                    }
                    int initial = i;
                    while (i < parsingUtils.len()) {
                        c = parsingUtils.charAt(i);
                        if (Character.isJavaIdentifierPart(c) || c == ' ' || c == '.'
                                || (acceptMultiLines && Character.isWhitespace(c))) {
                            i++;
                            continue;
                        }
                        if (c == '{' || c == '[' || c == '(' || c == '"' || c == '\'') {
                            i = parsingUtils.eatPar(i, null, c);
                            if (i < parsingUtils.len()) {
                                // checks first if we caught a closed partition
                                i++;
                                continue;
                            }
                            // it is not closed
                            return lst;
                        }
                        if (c == ',') {
                            variables.add(doc.get(initial, i - initial).trim());
                            i++;
                            initial = i;
                            continue;
                        }
                        break;
                    }
                    // format variables length has not been defined, so it is the end of the iteration
                    if (formatVariablesLen == -1) {
                        formatVariablesLen = i - partitionEndOffset;
                    }
                    // check if format variables ended with comma or if there is a variable left to capture
                    if (initial != i) {
                        String lastVarContent = doc.get(initial, i - initial).trim();
                        if (lastVarContent.length() > 0) {
                            variables.add(lastVarContent);
                        }
                    }
                    break;
                }

                // initialize format output string with the properly size allocation (f + string len + variables len)
                FastStringBuffer strBuf = new FastStringBuffer(1 + partition.getLength() + formatVariablesLen);
                strBuf.append('f').append(stringPartitionContents);

                // iterate through variables and edit the f-string output
                if (formatCount == variables.size()) {
                    for (String variable : variables) {
                        String rep = getReplace(strBuf);
                        String with = null;
                        if ("%r".equals(rep)) {
                            with = "{" + variable + "!r}";
                        } else {
                            with = "{" + variable + "}";
                        }
                        strBuf.replaceFirst(rep, with);
                    }
                } else if (variables.size() == 1) {
                    String variable = variables.get(0);
                    for (i = 0; i < formatCount; i++) {
                        String rep = getReplace(strBuf);
                        String with = null;
                        if ("%r".equals(rep)) {
                            with = "{" + variable + "[" + i + "]" + "!r}";
                        } else {
                            with = "{" + variable + "[" + i + "]" + "}";
                        }
                        strBuf.replaceFirst(rep, with);
                    }
                } else {
                    return lst;
                }
                lst.add(CompletionProposalFactory.get().createPyCompletionProposal(strBuf.toString(),
                        partitionOffset, partition.getLength() + formatVariablesLen, 0, getImage(imageCache,
                                UIConstants.COMPLETION_TEMPLATE),
                        "Convert to f-string", null, null, IPyCompletionProposal.PRIORITY_DEFAULT, null));
                return lst;
            } catch (SyntaxErrorException e) {
            }
        }
        // if we got here, it means that we do not have % after the string
        FastStringBuffer buf = new FastStringBuffer(stringPartitionContents.length() + 1);
        buf.append('f').append(stringPartitionContents);
        lst.add(CompletionProposalFactory.get().createPyCompletionProposal(buf.toString(),
                partitionOffset, partition.getLength(), 0, getImage(imageCache,
                        UIConstants.COMPLETION_TEMPLATE),
                "Convert to f-string", null, null, IPyCompletionProposal.PRIORITY_DEFAULT, null));
        return lst;
    }

    private String getReplace(FastStringBuffer strBuf) {
        String rep = "%s";
        int oS = strBuf.indexOf("%s");
        int oR = strBuf.indexOf("%r");
        if (oS == -1 || oR != -1 && oR < oS) {
            rep = "%r";
        }
        return rep;
    }

    private IImageHandle getImage(IImageCache imageCache, String c) {
        if (imageCache != null) {
            return imageCache.get(c);
        }
        return null;
    }

    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        if (edit != null) {
            try {
                IPythonNature pythonNature = edit.getPythonNature();
                if (pythonNature == null
                        || pythonNature.getGrammarVersion() < IPythonNature.GRAMMAR_PYTHON_VERSION_3_6) {
                    return false;
                }
            } catch (MisconfigurationException e) {
            }
        }
        return true;
    }
}
