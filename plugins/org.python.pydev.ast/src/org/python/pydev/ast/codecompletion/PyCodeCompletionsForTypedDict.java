package org.python.pydev.ast.codecompletion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.TokensOrProposalsList;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.partitioner.FastPartitioner;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class PyCodeCompletionsForTypedDict {

    /**
     * A new request generated after analyzing the new request.
     *
     * This is done because we do something as:
     *
     * x: SomeTypedDict = func(...)
     * x[''] <-- at this point we create a new completion request for `x` and then filter out what
     *           should be used as completions for the proper location.
     */
    private final Optional<CompletionRequest> artificialRequest;

    /**
     * This is the original request.
     * @throws BadLocationException
     */
    public PyCodeCompletionsForTypedDict(CompletionRequest request) throws BadLocationException {
        artificialRequest = createArtificialRequest(request);
    }

    public TokensOrProposalsList getStringCompletions() throws CoreException,
            BadLocationException, IOException, MisconfigurationException, PythonNatureWithoutProjectException,
            CompletionRecursionException {
        if (artificialRequest.isPresent()) {
            CompletionRequest request = artificialRequest.get();

            int line = request.doc.getLineOfOffset(request.documentOffset);
            IRegion region = request.doc.getLineInformation(line);
            ICompletionState state = new CompletionState(line, request.documentOffset - region.getOffset(), null,
                    request.nature, request.qualifier);
            state.setCancelMonitor(request.getCancelMonitor());
            state.setIsInCalltip(false);

            TokensList tokenCompletions = new TokensList();
            IPythonNature nature = request.nature;
            if (nature == null) {
                throw new RuntimeException("Unable to get python nature.");
            }

            ICodeCompletionASTManager astManager = nature.getAstManager();
            if (astManager == null) {
                //we're probably still loading it.
                return new TokensOrProposalsList();
            }

            String trimmed = request.getActivationToken().replace('.', ' ').trim();
            // We know it has to be a token completion, so, call it directly.
            PyCodeCompletion.doTokenCompletion(request, astManager, tokenCompletions, trimmed, state);
            TokensOrProposalsList tokensList = new TokensOrProposalsList();
            tokensList.addAll(tokenCompletions);

            List<ICompletionProposalHandle> completionProposals = new ArrayList<>();
            PyCodeCompletion.changeItokenToCompletionProposal(request, completionProposals, tokensList, false, state);
            TokensOrProposalsList ret = new TokensOrProposalsList(completionProposals);
            return ret;
        }
        return null;
    }

    public boolean isTypedDictCompletionRequest() {
        return artificialRequest.isPresent();
    }

    private Optional<CompletionRequest> createArtificialRequest(CompletionRequest request)
            throws BadLocationException {
        Optional<Tuple<String, Integer>> maybeQualifierAndOffset = getDictKeyQualifierAndOffset(request);
        if (maybeQualifierAndOffset.isPresent()) {
            Tuple<String, Integer> qualifierAndOffset = maybeQualifierAndOffset.get();
            Optional<String> activationToken = getActivationTokenForDictKey(request, qualifierAndOffset.o2);
            if (activationToken.isPresent()) {
                String parsedQualifier = qualifierAndOffset.o1;
                CompletionRequest artificialRequest = new CompletionRequest(
                        request.editorFile, request.nature, request.doc, activationToken.get(),
                        request.documentOffset, request.qlen, request.codeCompletion, parsedQualifier,
                        request.useSubstringMatchInCodeCompletion);

                artificialRequest.setCancelMonitor(request.getCancelMonitor());
                artificialRequest.filterToken = (name, tokenType) -> tokenType == IToken.TYPE_ATTR
                        && !(name.startsWith("__") && name.endsWith("__"));

                return Optional.of(artificialRequest);
            }
        }
        return Optional.empty();
    }

    private Optional<Tuple<String, Integer>> getDictKeyQualifierAndOffset(CompletionRequest request)
            throws BadLocationException {
        IDocumentPartitioner partitioner = PyPartitionScanner.checkPartitionScanner(request.doc);
        FastPartitioner fastPartitioner = (FastPartitioner) partitioner;
        final ITypedRegion partition = fastPartitioner.getPartition(request.documentOffset);
        String partitionType = partition.getType();

        if (IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE1.equals(partitionType)
                || IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE2.equals(partitionType)) {
            return Optional.of(getDictKeyQualifierAndOffsetForProbablyCorrectContents(request, partition));
        }
        return Optional.empty();
    }

    /**
     * Given a doc with:
     *
     * dct['foo'], this will return the string 'foo' as well as its offset (dct[|'foo']) <-- the '|'.
     */
    private Tuple<String, Integer> getDictKeyQualifierAndOffsetForProbablyCorrectContents(
            CompletionRequest request, final ITypedRegion partition) throws BadLocationException {
        final IDocument doc = request.doc;
        int qualifierOffset = partition.getOffset();
        int strContentLen = partition.getLength() - 2; // we have to ignore both of string identifiers (i.e.: `'` or `"`).
        String qualifier = "";
        if (strContentLen > 0) {
            // here we have a proper content for qualifier (open and closed string).
            qualifier = doc.get(qualifierOffset + 1, strContentLen); // extract only qualifier content (without string opener and closer).
        }
        Tuple<String, Integer> ret = new Tuple<String, Integer>(qualifier, qualifierOffset);
        return ret;
    }

    private Optional<String> getActivationTokenForDictKey(CompletionRequest request, int qualifierOffset)
            throws BadLocationException {
        final IDocument doc = request.doc;
        int i = qualifierOffset - 1;
        char c = doc.getChar(i);
        if (c == '[') {
            String activationToken = extractBeforeBracketActivationToken(i, doc);
            return Optional.of(activationToken);
        }
        return Optional.empty();
    }

    private String extractBeforeBracketActivationToken(int keyOffset, final IDocument doc)
            throws BadLocationException {
        Tuple<String, Integer> extractActivationToken = TextSelectionUtils.extractActivationToken(doc, keyOffset, true);
        String ret = extractActivationToken.o1;
        if (!ret.endsWith(".")) {
            ret += '.';
        }
        return ret;
    }

}
