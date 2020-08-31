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
import org.python.pydev.core.docutils.PySelection;
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

public class AssistFString implements IAssistProps {
    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature, IPyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        PySourceViewer viewer = null;
        if (edit instanceof PyEdit) { //only in tests it is actually null
            viewer = ((PyEdit) edit).getPySourceViewer();
        }
        List<ICompletionProposalHandle> l = new ArrayList<ICompletionProposalHandle>();

        IDocument doc = ps.getDoc();
        FastPartitioner fastPartitioner = (FastPartitioner) PyPartitionScanner.checkPartitionScanner(doc);
        ITypedRegion partition = fastPartitioner.getPartition(offset);

        FastStringBuffer strBuf = new FastStringBuffer();
        strBuf.append('f').append(doc.get(partition.getOffset(), partition.getLength()));

        FastStringBuffer variablesBuf = new FastStringBuffer();
        // get string end offset
        int partitionEndOffset = partition.getOffset() + partition.getLength();
        // get format (% ....) end offset
        int formatEndOffset = doc.getLineLength(doc.getLineOfOffset(partitionEndOffset));
        // set a buf exclusively for the format variables
        variablesBuf.append(doc.get(partitionEndOffset, formatEndOffset - partitionEndOffset));

        // get only the variable names and remove the last comma
        variablesBuf.trim().deleteFirst();
        if (variablesBuf.trim().startsWith('(')) {
            variablesBuf.deleteFirst();
            variablesBuf.deleteLast();
            variablesBuf.trim();
            // if we caught the last parenthesis of a call, adjust the format end offset
            while (variablesBuf.endsWith(')')) {
                variablesBuf.deleteLast();
                variablesBuf.trim();
                // it considers the opened parenthesis
                formatEndOffset -= 2;
            }
        }
        if (variablesBuf.endsWith(',')) {
            variablesBuf.deleteLast();
            variablesBuf.trim();
        }

        // get variables without literals
        String variablesWithoutLiterals = PySelection.getLineWithoutCommentsOrLiterals(variablesBuf.toString());
        // get variable by variable and edit the f-string output
        int i = 0;
        while (true) {
            int commaPos = variablesWithoutLiterals.indexOf(',', i);
            if (commaPos != -1) {
                CharSequence subSequence = variablesBuf.subSequence(i, commaPos);
                strBuf.replaceFirst("%s", "{" + subSequence.toString().trim() + "}");
                i = commaPos + 1;
            } else {
                CharSequence subSequence = variablesBuf.subSequence(i, variablesBuf.length());
                strBuf.replaceFirst("%s", "{" + subSequence.toString().trim() + "}");
                break;
            }
        }
        l.add(CompletionProposalFactory.get().createPyCompletionProposal(strBuf.toString(),
                partition.getOffset(), formatEndOffset - partition.getOffset(), 0, getImage(imageCache,
                        UIConstants.COMPLETION_TEMPLATE),
                "Convert to f-string", null, null, IPyCompletionProposal.PRIORITY_DEFAULT, null));
        return l;
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
