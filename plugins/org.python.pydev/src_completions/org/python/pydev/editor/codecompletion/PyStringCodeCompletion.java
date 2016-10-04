/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.DocIterator;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

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
     */
    @Override
    @SuppressWarnings("unchecked")
    public List getCodeCompletionProposals(ITextViewer viewer, CompletionRequest request) throws CoreException,
            BadLocationException, MisconfigurationException {
        ArrayList ret = new ArrayList();
        request.showTemplates = false; //don't show templates in strings
        fillWithEpydocFields(viewer, request, ret);

        if (ret.size() == 0) {
            //if the size is not 0, it means that this is a place for the '@' stuff, and not for the 'default' context for a string.
            ret.addAll(getStringGlobalsFromParticipants(request, CompletionStateFactory.getEmptyCompletionState(
                    request.activationToken, request.nature, new CompletionCache())));

            //the code-below does not work well because the module may not have an actual import for the activation token,
            //so, it is useless too many times
            //if(request.activationToken.length() != 0){
            //    PyCodeCompletion completion = new PyCodeCompletion();
            //    ret.addAll(completion.getCodeCompletionProposals(viewer, request));
            //}
        }

        fillWithParams(viewer, request, ret);
        return ret;
    }

    /**
     * @param ret OUT: this is where the completions are stored
     */
    private void fillWithParams(ITextViewer viewer, CompletionRequest request, ArrayList<ICompletionProposal> ret) {
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
                                ret.add(new PyLinkedModeCompletionProposal(str, request.documentOffset - request.qlen,
                                        request.qlen, str.length(), PyCodeCompletionImages
                                                .getImageForType(IToken.TYPE_PARAM),
                                        null, null, "", 0,
                                        PyCompletionProposal.ON_APPLY_DEFAULT, "", null));
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
    private void fillWithEpydocFields(ITextViewer viewer, CompletionRequest request,
            ArrayList<ICompletionProposal> ret) {
        try {
            Region region = new Region(request.documentOffset - request.qlen, request.qlen);
            Image image = PyCodeCompletionImages.getImageForType(IToken.TYPE_EPYDOC);
            TemplateContext context = createContext(viewer, region, request.doc);

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
                        ret.add(new TemplateProposal(t, context, region, image, 5) {
                            @Override
                            public String getDisplayString() {
                                if (SharedCorePlugin.inTestMode()) {
                                    return this.getPrefixCompletionText(null, 0).toString();
                                } else {
                                    return super.getDisplayString();
                                }
                            }
                        });
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
    private Collection getStringGlobalsFromParticipants(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException {
        ArrayList ret = new ArrayList();

        List participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
        for (Iterator iter = participants.iterator(); iter.hasNext();) {
            IPyDevCompletionParticipant participant = (IPyDevCompletionParticipant) iter.next();
            ret.addAll(participant.getStringGlobalCompletions(request, state));
        }
        return ret;
    }

}
