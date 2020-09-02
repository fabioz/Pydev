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

        final String stringPartitionContents = doc.get(partition.getOffset(), partition.getLength());
        int formatCount = StringUtils.count(stringPartitionContents, "%s");
        if (formatCount == 0) {
            return lst;
        }

        // get string end offset
        int partitionEndOffset = partition.getOffset() + partition.getLength();
        // get format end offset
        int i = partitionEndOffset;
        int formatVariablesLen = -1;
        boolean acceptMultiLines = false;
        try {
            List<String> variables = new ArrayList<String>();
            ParsingUtils parsingUtils = ParsingUtils.create(doc);
            while (i < parsingUtils.len()) {
                char c = parsingUtils.charAt(i);
                if (Character.isWhitespace(c) || c == '%') {
                    i++;
                    continue;
                }
                if (c == '{') {
                    return lst;
                }
                if (c == '[' || c == '(') {
                    formatVariablesLen = parsingUtils.eatPar(i, null, c) - partitionEndOffset + 1;
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
                        i = parsingUtils.eatPar(i, null, c) + 1;
                        continue;
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
                    variables.add(doc.get(initial, i - initial).trim());
                }
                break;
            }

            // initialize format output string with the properly size allocation (f + string len + variables len)
            FastStringBuffer strBuf = new FastStringBuffer(1 + partition.getLength() + formatVariablesLen);
            strBuf.append('f').append(stringPartitionContents);

            // iterate through variables and edit the f-string output
            if (formatCount == variables.size()) {
                for (String variable : variables) {
                    strBuf.replaceFirst("%s", "{" + variable + "}");
                }
            } else if (variables.size() == 1) {
                String variable = variables.get(0);
                for (i = 0; i < formatCount; i++) {
                    strBuf.replaceFirst("%s", "{" + variable + "[" + i + "]" + "}");
                }
            } else {
                return lst;
            }
            lst.add(CompletionProposalFactory.get().createPyCompletionProposal(strBuf.toString(),
                    partition.getOffset(), partition.getLength() + formatVariablesLen, 0, getImage(imageCache,
                            UIConstants.COMPLETION_TEMPLATE),
                    "Convert to f-string", null, null, IPyCompletionProposal.PRIORITY_DEFAULT, null));
        } catch (SyntaxErrorException e) {
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
