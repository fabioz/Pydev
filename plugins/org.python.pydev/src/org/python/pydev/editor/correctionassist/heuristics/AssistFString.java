package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.IAssistProps;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.partitioner.FastPartitioner;

public class AssistFString implements IAssistProps {

    private final Pattern STRING_TO_FORMAT_PATTERN = Pattern
            .compile(
                    "(\\%\\s*(?:[\\+\\-]\\s*\\d*|\\d*|\\*)?(?:\\.\\d*|\\.\\*)?[adiouxXeEfFgGcrs]|\\%\\(.*\\)\\s*[adiouxXeEfFgGcrs])");

    private static class VariableToFormat {
        private final int relativeOffset;
        private String representation;

        public VariableToFormat(int offset) {
            super();
            this.relativeOffset = offset;
        }
    }

    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature, IPyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        PySourceViewer viewer = null;
        if (edit != null) { //only in tests it's actually null
            viewer = ((PyEdit) edit).getPySourceViewer();
        }
        List<ICompletionProposalHandle> l = new ArrayList<ICompletionProposalHandle>();

        //

        return l;
    }

    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        if (ps.getTextSelection().getLength() == 0 && edit != null && edit.getAST() != null) {
            IDocument doc = ps.getDoc();
            SimpleNode node = (SimpleNode) edit.getAST();
            ITypedRegion partition = ((FastPartitioner) PyPartitionScanner.checkPartitionScanner(doc))
                    .getPartition(offset);

            if (IPythonPartitions.STRING_PARTITIONS.contains(partition.getType())) {
                try {
                    String partitionContent = doc.get(partition.getOffset(), partition.getLength());

                    Matcher strMatcher = STRING_TO_FORMAT_PATTERN.matcher(partitionContent);

                    stmtType[] body = NodeUtils.getBody(node);
                    for (stmtType content : body) {
                        if (content != null) {
                            BinOp value = null;
                            if (content instanceof Assign && ((Assign) content).value != null
                                    && ((Assign) content).value instanceof BinOp) {
                                value = (BinOp) ((Assign) content).value;
                            } else if (content instanceof Expr && ((Expr) content).value != null
                                    && ((Expr) content).value instanceof BinOp) {
                                value = (BinOp) ((Expr) content).value;
                            }

                            if (value != null) {
                                int beginOffset = PySelection.getAbsoluteCursorOffset(doc, value.left.beginLine - 1,
                                        value.left.beginColumn - 1);
                                if (beginOffset == partition.getOffset()) {
                                    if (value.right != null) {
                                        if (value.right instanceof Name) {
                                            int i = 0;
                                            while (strMatcher.find()) {
                                                if (i > 0) {
                                                    return false;
                                                }
                                            }

                                        } else if (value.right instanceof Tuple) {

                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } catch (BadLocationException e) {
                    return false;
                }

            }
        }
        return false;
    }
}
