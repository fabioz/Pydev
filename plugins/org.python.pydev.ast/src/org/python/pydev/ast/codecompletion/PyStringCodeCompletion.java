/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.python.pydev.ast.codecompletion.revisited.CompletionCache;
import org.python.pydev.ast.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TokensOrProposalsList;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.partition.PyPartitionScanner;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.parser.fastparser.grammar_fstrings_common.FStringsAST;
import org.python.pydev.parser.fastparser.grammar_fstrings_common.SimpleNode;
import org.python.pydev.parser.grammar_fstrings.FStringsGrammarFactory;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.partitioner.FastPartitioner;
import org.python.pydev.shared_core.string.DocIterator;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * The code-completion engine that should be used inside strings
 *
 * @author fabioz
 */
public class PyStringCodeCompletion extends AbstractTemplateCodeCompletion {

    /**
     * Epydoc fields (after @)
     */
    public static String[] EPYDOC_FIELDS = new String[] {
            //function or method
            "param",
            "param ${param}: ${cursor}",
            "A description of the parameter for a function or method.",
            "type",
            "type ${param}: ${cursor}",
            "The expected type for the parameter, var or property",
            "return",
            "return: ",
            "The return value for a function or method.",
            "rtype",
            "rtype: ",
            "The type of the return value for a function or method.",
            "keyword",
            "keyword ${param}: ${cursor}",
            "A description of the keyword parameter.",
            "raise",
            "raise ${exception}: ${cursor}",
            "A description of the circumstances under which a function or method raises an exception",

            //class or module
            "ivar",
            "ivar ${ivar}: ${cursor}",
            "A description of a class instance variable",
            "cvar",
            "cvar ${cvar}: ${cursor}",
            "A description of a static class variable",
            "var",
            "var ${var}: ${cursor}",
            "A description of a module variable",

            "group",
            "group ${group}: ${cursor}",
            "Organizes a set of related children of a module or class into a group. g is the name of the group; and c1,...,cn are the names of the children in the group. To define multiple groups, use multiple group fields.",
            "sort",
            "sort: ",
            "Specifies the sort order for the children of a module or class. c1,...,cn are the names of the children, in the order in which they should appear. Any children that are not included in this list will appear after the children from this list, in alphabetical order.",

            "see",
            "see: ",
            "A description of a related topic. see fields typically use documentation crossreference links or external hyperlinks that link to the related topic.",
            "note",
            "note: ",
            "A note about an object. Multiple note fields may be used to list separate notes.",
            "attention",
            "attention: ",
            "An important note about an object. Multiple attention fields may be used to list separate notes.",
            "bug",
            "bug: ",
            "A description of a bug in an object. Multiple bug fields may be used to report separate bugs.",
            "warning",
            "warning: ",
            "A warning about an object. Multiple warning fields may be used to report separate warnings.",

            "version",
            "version: ",
            "The current version of an object.",
            "todo",
            "todo: ",
            "A planned change to an object. If the optional argument ver  is given, then it specifies the version for which the change will be made. Multiple todo fields may be used if multiple changes are planned.",
            "deprecated",
            "deprecated: ",
            "Indicates that an object is deprecated. The body of the field describe the reason why the object is deprecated.",
            "since",
            "since: ",
            "The date or version when an object was first introduced.",
            "status",
            "status: ",
            "The current status of an object.",
            "change",
            "change: ",
            "A change log entry for this object.",

            "requires",
            "requires: ",
            "A requirement for using an object. Multiple requires  fields may be used if an object has multiple requirements.",
            "precondition",
            "precondition: ",
            "A condition that must be true before an object is used. Multiple precondition fields may be used if an object has multiple preconditions.",
            "postcondition",
            "postcondition: ",
            "A condition that is guaranteed to be true after an object is used. Multiple postcondition fields may be used if an object has multiple postconditions.",
            "invariant",
            "invariant: ",
            "A condition which should always be true for an object. Multiple invariant fields may be used if an object has multiple invariants.",

            "author",
            "author: ",
            "The author(s) of an object. Multiple author  fields may be used if an object has multiple authors.",
            "organization",
            "organization: ",
            "The organization that created or maintains an object.",
            "copyright",
            "copyright: ",
            "The copyright information for an object.",
            "license",
            "license: ",
            "The licensing information for an object.",
            "contact",
            "contact: ",
            "Contact information for the author or maintainer of a module, class, function, or method. Multiple contact fields may be used if an object has multiple contacts.",
            "summary",
            "summary: ",
            "A summary description for an object. This description overrides the default summary (which is constructed from the first sentence of the object's description).", };

    /**
     * Needed interface for adding the completions on a request
     * @throws MisconfigurationException
     * @throws PythonNatureWithoutProjectException
     * @throws IOException
     */
    @Override
    public TokensOrProposalsList getCodeCompletionProposals(CompletionRequest request) throws CoreException,
            BadLocationException, MisconfigurationException, IOException, PythonNatureWithoutProjectException {

        List<ICompletionProposalHandle> completionProposals = new ArrayList<>();
        request.showTemplates = false; //don't show templates in strings
        fillWithEpydocFields(request, completionProposals);

        TokensOrProposalsList ret = new TokensOrProposalsList();

        if (completionProposals.size() == 0) {
            //if the size is not 0, it means that this is a place for the '@' stuff, and not for the 'default' context for a string.

            IDocument doc = request.doc;
            FastPartitioner fastPartitioner = ((FastPartitioner) PyPartitionScanner.checkPartitionScanner(doc));
            ITypedRegion partition = fastPartitioner.getPartition(request.documentOffset);

            String partitionType = partition.getType();
            if (IPythonPartitions.F_STRING_PARTITIONS.contains(partitionType)) {
                // Now we are going to check whether where we are in the given completion offset
                int requestOffset = request.documentOffset;
                int partitionOffset = partition.getOffset();
                int partitionLine = doc.getLineOfOffset(partitionOffset);
                int partitionCol = partitionOffset - doc.getLineOffset(partitionLine);

                String str = doc.get(partitionOffset, partition.getLength());

                FStringsAST ast = null;
                try {
                    ast = FStringsGrammarFactory.createGrammar(str).f_string();
                } catch (Throwable e) {
                    // Just ignore any errors for this.
                }

                if (ast != null && ast.hasChildren()) {
                    for (SimpleNode node : ast.getBalancedExpressionsToBeEvaluatedInRegularGrammar()) {
                        int nodeOffset;
                        int nodeEndOffset;
                        if (node.beginLine > 1) {
                            nodeOffset = TextSelectionUtils.getAbsoluteCursorOffset(doc,
                                    partitionLine + node.beginLine - 1, node.beginColumn - 1);
                        } else {
                            nodeOffset = TextSelectionUtils.getAbsoluteCursorOffset(doc,
                                    partitionLine + node.beginLine - 1, partitionCol + node.beginColumn - 1);
                        }
                        if (node.endLine > 1) {
                            nodeEndOffset = TextSelectionUtils.getAbsoluteCursorOffset(doc,
                                    partitionLine + node.endLine - 1, node.endColumn);
                        } else {
                            nodeEndOffset = TextSelectionUtils.getAbsoluteCursorOffset(doc,
                                    partitionLine + node.endLine - 1, partitionCol + node.endColumn);
                        }

                        if (requestOffset >= nodeOffset && requestOffset <= nodeEndOffset) {
                            // request is inside a format, so we have to get a normal code completion to it
                            return new PyCodeCompletion().getCodeCompletionProposals(request);
                        }
                    }
                }
            }

            PyCodeCompletionsForTypedDict pyCodeCompletionsForTypedDict = new PyCodeCompletionsForTypedDict(request);
            if (pyCodeCompletionsForTypedDict.isTypedDictCompletionRequest()) {
                // If it's a typed dict completion request, don't go into other requests.
                TokensOrProposalsList completionsForTypedDict;
                try {
                    completionsForTypedDict = pyCodeCompletionsForTypedDict.getStringCompletions();
                    if (completionsForTypedDict != null) {
                        return completionsForTypedDict;
                    }
                } catch (CompletionRecursionException e) {
                    Log.log(e);
                }
                return new TokensOrProposalsList();
            }

            TokensOrProposalsList stringGlobalsFromParticipants = getStringGlobalsFromParticipants(request,
                    CompletionStateFactory.getEmptyCompletionState(
                            request.activationToken, request.nature, new CompletionCache()));
            ret.addAll(stringGlobalsFromParticipants);

            //the code-below does not work well because the module may not have an actual import for the activation token,
            //so, it is useless too many times
            //if(request.activationToken.length() != 0){
            //    PyCodeCompletion completion = new PyCodeCompletion();
            //    ret.addAll(completion.getCodeCompletionProposals(viewer, request));
            //}
        }

        fillWithParams(request, completionProposals);
        ret.addAll(new TokensOrProposalsList(completionProposals));
        return ret;
    }

    /**
     * @param ret OUT: this is where the completions are stored
     */
    private void fillWithParams(CompletionRequest request, List<ICompletionProposalHandle> ret) {
        PySelection ps = new PySelection(request.doc, request.documentOffset);
        try {
            String lineContentsToCursor = ps.getLineContentsToCursor();
            String trimmed = lineContentsToCursor.trim();

            //only add params on param and type tags
            if (!trimmed.startsWith("@param") && !trimmed.startsWith("@type") && !trimmed.startsWith(":param")
                    && !trimmed.startsWith(":type")) {
                return;
            }

            //for params, we never have an activation token (just a qualifier)
            if (request.activationToken.trim().length() != 0) {
                return;
            }

            String initial = request.qualifier;

            DocIterator iterator = new DocIterator(false, ps);
            while (iterator.hasNext()) {
                String line = iterator.next().trim();
                if (line.startsWith("def ")) {
                    int currentLine = iterator.getCurrentLine() + 1;
                    PySelection selection = new PySelection(request.doc, currentLine, 0);
                    if (selection.isInFunctionLine(true)) {
                        Tuple<List<String>, Integer> insideParentesisToks = selection.getInsideParentesisToks(false);
                        for (String str : insideParentesisToks.o1) {
                            if (str.startsWith(initial)) {
                                ret.add(CompletionProposalFactory.get().createPyLinkedModeCompletionProposal(str,
                                        request.documentOffset - request.qlen, request.qlen, str.length(),
                                        PyCodeCompletionImages
                                                .getImageForType(IToken.TYPE_PARAM),
                                        null, null, "", 0,
                                        IPyCompletionProposal.ON_APPLY_DEFAULT, "", null));
                            }
                        }
                        return;
                    }
                }
            }

        } catch (BadLocationException e) {
        }
    }

    /**
     * @param ret OUT: this is where the completions are stored
     */
    private void fillWithEpydocFields(CompletionRequest request,
            List<ICompletionProposalHandle> ret) {
        try {
            Region region = new Region(request.documentOffset - request.qlen, request.qlen);
            IImageHandle image = PyCodeCompletionImages.getImageForType(IToken.TYPE_EPYDOC);
            TemplateContext context = createContext(region, request.doc);

            char c = request.doc.getChar(request.documentOffset - request.qualifier.length() - 1);

            boolean createFields = c == '@' || c == ':';
            if (createFields) {
                String lineContentsToCursor = PySelection.getLineContentsToCursor(request.doc,
                        request.documentOffset - request.qualifier.length() - 1);
                if (lineContentsToCursor.trim().length() != 0) {
                    //Only create if @param or :param is the first thing in the line.
                    createFields = false;
                }
            }
            if (createFields) {
                //ok, looking for epydoc filters
                for (int i = 0; i < EPYDOC_FIELDS.length; i++) {
                    String f = EPYDOC_FIELDS[i];
                    if (f.startsWith(request.qualifier)) {
                        Template t = new Template(f, EPYDOC_FIELDS[i + 2], "", EPYDOC_FIELDS[i + 1], false);
                        ret.add(
                                CompletionProposalFactory.get().createPyTemplateProposalForTests(
                                        t, context, region, image, 5));
                    }
                    i += 2;
                }
            }
        } catch (BadLocationException e) {
            //just ignore it
        }
    }

    /**
     * @return completions added from contributors
     * @throws MisconfigurationException
     */
    private TokensOrProposalsList getStringGlobalsFromParticipants(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException {
        TokensOrProposalsList ret = new TokensOrProposalsList();

        @SuppressWarnings("unchecked")
        List<IPyDevCompletionParticipant> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_COMPLETION);
        for (Iterator<IPyDevCompletionParticipant> iter = participants.iterator(); iter.hasNext();) {
            IPyDevCompletionParticipant participant = iter.next();
            ret.addAll(participant.getStringGlobalCompletions(request, state));
        }
        return ret;
    }

}
