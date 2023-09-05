/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.codecompletion.ProposalsComparator.CompareContext;
import org.python.pydev.ast.codecompletion.revisited.AbstractToken;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ICompletionState.LookingFor;
import org.python.pydev.core.IFilterToken;
import org.python.pydev.core.IToken;
import org.python.pydev.core.IterEntry;
import org.python.pydev.core.TokensOrProposalsList;
import org.python.pydev.core.docutils.ImportsSelection;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal.ICompareContext;
import org.python.pydev.shared_core.string.FastStringBuffer;

public abstract class AbstractPyCodeCompletion implements IPyCodeCompletion {

    /* (non-Javadoc)
     * @see org.python.pydev.ast.codecompletion.IPyCodeCompletion#getImportsTipperStr(org.python.pydev.ast.codecompletion.CompletionRequest)
     */
    @Override
    public ImportInfo getImportsTipperStr(CompletionRequest request) {

        IDocument doc = request.doc;
        int documentOffset = request.documentOffset;

        return ImportsSelection.getImportsTipperStr(doc, documentOffset);
    }

    /**
     * This is the place where we change the tokens we've gathered so far with the 'inference' engine and transform those
     * tokens to actual completions as requested by the Eclipse infrastructure.
     * @param lookingForInstance if looking for instance, we should not add the 'self' as parameter.
     */
    public static void changeItokenToCompletionProposal(CompletionRequest request,
            List<ICompletionProposalHandle> convertedProposals, TokensOrProposalsList iTokenList, boolean importsTip,
            ICompletionState state) {

        FastStringBuffer result = new FastStringBuffer();
        FastStringBuffer temp = new FastStringBuffer();

        int replacementOffset = request.documentOffset - request.qlen;

        int forcedContextInformationOffset = -1;

        //that's negated so that we can use it as an integer later on (to sum it)
        int notInCalltip = 1;
        int onApplyAction = IPyCompletionProposal.ON_APPLY_DEFAULT;
        if (request.isInCalltip) {
            notInCalltip = 0; //when we're in the calltip, we don't have to add a char '(' to the start of the context information.
            if (request.alreadyHasParams) {
                onApplyAction = IPyCompletionProposal.ON_APPLY_JUST_SHOW_CTX_INFO;
                forcedContextInformationOffset = request.calltipOffset;

            } else {
                onApplyAction = IPyCompletionProposal.ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS;
            }
        }

        IFilterToken filterToken = request.filterToken;
        int i = 0;
        for (Iterator<IterEntry> iter = iTokenList.iterator(); iter.hasNext();) {
            i++;
            if (i > 10000) {
                break;
            }

            IterEntry entry = iter.next();
            Object obj = entry.object;

            if (obj instanceof IToken) {
                IToken element = (IToken) obj;

                String name = element.getRepresentation();
                if (filterToken != null && !filterToken.accept(name, element.getType())) {
                    continue;
                }

                //GET the ARGS
                int l = name.length();

                boolean nameIsFullQualifier = name.equals(request.fullQualifier);
                String args = "";
                if (!importsTip) {
                    boolean getIt = true;
                    if (AbstractToken.isClassDef(element) && !nameIsFullQualifier) {
                        if (!request.isInCalltip) {
                            getIt = false;
                        }
                    } else if (AbstractToken.isFunctionDefProperty(element)) {
                        getIt = false;
                    }
                    if (getIt) {
                        args = getArgs(element, entry.lookingFor);
                        if (args.length() > 0) {
                            l++; //cursor position is name + '('
                        }
                    }
                }
                //END

                if (nameIsFullQualifier && args.trim().length() == 0) {
                    //we don't want to get the tokens that are equal to the current 'full' qualifier
                    //...unless it adds the parameters to a call...
                    continue;
                }

                int type = element.getType();

                int priority = IPyCompletionProposal.PRIORITY_DEFAULT;
                if (type == IToken.TYPE_PARAM || type == IToken.TYPE_LOCAL
                        || type == IToken.TYPE_OBJECT_FOUND_INTERFACE) {
                    priority = IPyCompletionProposal.PRIORITY_LOCALS;
                }

                Object pyContextInformation = null;
                if (args.length() > 2) {
                    int contextInformationOffset;
                    if (forcedContextInformationOffset < 0) {
                        contextInformationOffset = replacementOffset + name.length() + notInCalltip;
                    } else {
                        contextInformationOffset = forcedContextInformationOffset;
                    }
                    pyContextInformation = CompletionProposalFactory.get().createPyCalltipsContextInformationFromIToken(
                            element, args,
                            contextInformationOffset); //just after the parenthesis
                }

                String replacementString = name + makeArgsForDocumentReplacement(args, result, temp);
                String displayString = name + args;
                ICompareContext compareContext = new CompareContext(element);
                ICompletionProposalHandle proposal = CompletionProposalFactory.get()
                        .createPyLinkedModeCompletionProposal(
                                replacementString, replacementOffset, request.qlen,
                                l, element, displayString, pyContextInformation, priority, onApplyAction, args,
                                compareContext);

                convertedProposals.add(proposal);

            } else if (obj instanceof Object[]) {
                Object element[] = (Object[]) obj;

                String name = (String) element[0];
                String docStr = (String) element[1];
                int type = -1;
                if (element.length > 2) {
                    type = ((Integer) element[2]).intValue();
                }

                if (filterToken != null && !filterToken.accept(name, type)) {
                    continue;
                }

                int priority = IPyCompletionProposal.PRIORITY_DEFAULT;
                if (type == IToken.TYPE_PARAM) {
                    priority = IPyCompletionProposal.PRIORITY_LOCALS;
                }

                ICompletionProposalHandle proposal = CompletionProposalFactory.get().createPyCompletionProposal(name,
                        request.documentOffset - request.qlen, request.qlen,
                        name.length(), PyCodeCompletionImages.getImageForType(type), null, null, docStr, priority,
                        null);

                convertedProposals.add(proposal);

            } else if (obj instanceof ICompletionProposalHandle && filterToken == null) {
                //no need to convert
                convertedProposals.add((ICompletionProposalHandle) obj);
            }
        }
    }

    private static int STATE_INITIAL = 0;
    private static int STATE_FOUND_CHAR = 1;
    private static int STATE_FOUND_WHITESPACE = 2;
    private static int STATE_FOUND_WHITESPACE_AFTER_CHAR = 3;

    /**
     * Converts the arguments received to arguments to be added to the document. See tests for examples.
     *
     * result and temp are the buffers that are used in this function to build the arguments. They are cleared
     * before use (this is an optimization so that we don't need to recreate them at each time here as it's
     * called within a loop).
     */
    public static String makeArgsForDocumentReplacement(String args, FastStringBuffer result, FastStringBuffer temp) {
        result = result.clear();
        temp = temp.clear();

        int state = STATE_INITIAL;
        int starsToAdd = 0;

        for (char c : args.toCharArray()) {
            if (c == '*') {
                starsToAdd++;
                continue;
            }
            if (c == ',' || c == '(' || c == ')') {
                appendTempToResult(result, temp, starsToAdd);
                result.append(c);
                temp.clear();
                starsToAdd = 0;
                state = STATE_INITIAL;
            } else {
                if (Character.isWhitespace(c)) {
                    if (state == STATE_FOUND_CHAR) {
                        state = STATE_FOUND_WHITESPACE_AFTER_CHAR;

                    } else if (state != STATE_FOUND_WHITESPACE_AFTER_CHAR) {
                        state = STATE_FOUND_WHITESPACE;
                    }
                    continue;
                } else {
                    if (state == STATE_FOUND_WHITESPACE_AFTER_CHAR) {
                        temp.clear();
                    }
                    state = STATE_FOUND_CHAR;
                }
                temp.append(c);
            }
        }
        appendTempToResult(result, temp, starsToAdd);
        return result.toString();
    }

    private static void appendTempToResult(FastStringBuffer result, FastStringBuffer temp, int starsToAdd) {
        if (result.toString().trim().endsWith(",")) {
            result.append(' ');
        }
        result.appendN('*', starsToAdd);
        result.append(temp);
    }

    private static String getArgs(IToken element, LookingFor lookingFor) {
        return getArgs(element.getArgs(), element.getType(), lookingFor);
    }

    /**
     * @return a string with the arguments to be shown for the given element.
     *
     * E.g.: >>(self, a, b)<< Returns (a, b)
     */
    public static String getArgs(String argsReceived, int type, LookingFor lookingFor) {
        String args = "";
        boolean lookingForInstance = lookingFor == ICompletionState.LookingFor.LOOKING_FOR_INSTANCE_UNDEFINED
                || lookingFor == null
                || lookingFor == ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE
                || lookingFor == ICompletionState.LookingFor.LOOKING_FOR_ASSIGN;
        String trimmed = argsReceived.trim();
        if (trimmed.length() > 0) {
            FastStringBuffer buffer = new FastStringBuffer("(", 128);

            char c = trimmed.charAt(0);
            if (c == '(') {
                trimmed = trimmed.substring(1);
            }
            if (trimmed.length() > 0) {
                c = trimmed.charAt(trimmed.length() - 1);
                if (c == ')') {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
            }
            trimmed = trimmed.trim();

            //Now, if it starts with self or cls, we may have to remove it.
            String temp;
            if (lookingForInstance && trimmed.startsWith("self")) {
                temp = trimmed.substring(4);

            } else if (trimmed.startsWith("cls")) {
                temp = trimmed.substring(3);
            } else {
                temp = trimmed;
            }
            temp = temp.trim();
            if (temp.length() > 0) {
                //but only if it wasn't a self or cls followed by a valid identifier part.
                if (!Character.isJavaIdentifierPart(temp.charAt(0))) {
                    trimmed = temp;
                }
            } else {
                trimmed = temp;
            }

            trimmed = trimmed.trim();
            if (trimmed.startsWith(",")) {
                trimmed = trimmed.substring(1);
            }
            trimmed = trimmed.trim();
            buffer.append(trimmed);

            buffer.append(")");
            args = buffer.toString();
        } else if (type == IToken.TYPE_FUNCTION) {
            args = "()";
        }

        return args;
    }

}
