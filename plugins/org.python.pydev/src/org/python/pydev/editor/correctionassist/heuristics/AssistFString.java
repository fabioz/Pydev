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

public class AssistFString implements IAssistProps {
    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature, IPyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        PySourceViewer viewer = null;
        if (edit != null) { //only in tests it is actually null
            viewer = ((PyEdit) edit).getPySourceViewer();
        }
        List<ICompletionProposalHandle> l = new ArrayList<ICompletionProposalHandle>();

        IDocument doc = ps.getDoc();
        ITypedRegion partition = ((FastPartitioner) PyPartitionScanner.checkPartitionScanner(doc))
                .getPartition(offset);

        FastStringBuffer strBuf = new FastStringBuffer();
        strBuf.append(doc.get(partition.getOffset(), partition.getLength()));

        FastStringBuffer variablesBuf = new FastStringBuffer();
        try {
            int partitionEndOffset = partition.getOffset() + partition.getLength();
            int formatEnd = ParsingUtils.create(doc).eatPar(partitionEndOffset, null, '(');
            variablesBuf.append(doc.get(partitionEndOffset, formatEnd - partitionEndOffset));

            if (variablesBuf.indexOf('(') != -1) {
                variablesBuf.deleteFirstChars(variablesBuf.indexOf('(') + 1);
                variablesBuf.deleteLastChars(variablesBuf.length() - variablesBuf.indexOf(')'));
            } else {
                variablesBuf.deleteFirstChars(variablesBuf.indexOf('%') + 1);
            }

            if (variablesBuf.trim().endsWith(',')) {
                variablesBuf.deleteLast();
                variablesBuf.trim();
            }

            while (true) {
                int commaPos = variablesBuf.indexOf(',');
                if (commaPos != -1) {
                    CharSequence subSequence = variablesBuf.subSequence(0, commaPos);
                    strBuf.replaceFirst("%s", "{" + subSequence.toString().trim() + "}");
                    variablesBuf.deleteFirstChars(commaPos + 1);
                } else {
                    CharSequence subSequence = variablesBuf.subSequence(0, variablesBuf.length());
                    strBuf.replaceFirst("%s", "{" + subSequence.toString().trim() + "}");
                    break;
                }
            }
            l.add(CompletionProposalFactory.get().createAssistAssignCompletionProposal(
                    strBuf.insert(0, 'f').toString(),
                    partition.getOffset(), formatEnd - partition.getOffset(), 0, getImage(imageCache,
                            UIConstants.COMPLETION_TEMPLATE),
                    "Convert to f-string", null, null, IPyCompletionProposal.PRIORITY_DEFAULT, viewer,
                    null));
        } catch (SyntaxErrorException e) {
            e.printStackTrace();
        }

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
