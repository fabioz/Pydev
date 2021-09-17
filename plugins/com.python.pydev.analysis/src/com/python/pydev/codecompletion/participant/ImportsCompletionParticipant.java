/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 21/08/2005
 */
package com.python.pydev.codecompletion.participant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.ast.codecompletion.CompletionRequest;
import org.python.pydev.ast.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.ast.codecompletion.IPyDevCompletionParticipant2;
import org.python.pydev.ast.codecompletion.ProposalsComparator.CompareContext;
import org.python.pydev.ast.codecompletion.PyCodeCompletionPreferences;
import org.python.pydev.ast.codecompletion.PyCodeCompletionUtils;
import org.python.pydev.ast.codecompletion.PyCodeCompletionUtils.IFilter;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IInfo;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.TokensOrProposalsList;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQualifier;
import org.python.pydev.core.interactive_console.IScriptConsoleViewer;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal.ICompareContext;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;

import com.python.pydev.analysis.AnalysisPreferences;

/**
 * Gathers completions from the modules available (for the editor or for the console).
 *
 * @author Fabio
 */
public class ImportsCompletionParticipant implements IPyDevCompletionParticipant, IPyDevCompletionParticipant2 {

    // Console completions ---------------------------------------------------------------------------------------------

    @Override
    public Collection<ICompletionProposalHandle> computeConsoleCompletions(ActivationTokenAndQualifier tokenAndQual,
            Set<IPythonNature> naturesUsed, IScriptConsoleViewer viewer, int requestOffset) {
        ArrayList<ICompletionProposalHandle> completions = new ArrayList<ICompletionProposalHandle>();

        if (tokenAndQual.activationToken != null && tokenAndQual.activationToken.length() > 0) {
            //we only want
            return completions;
        }

        String qual = tokenAndQual.qualifier;
        if (qual.length() >= PyCodeCompletionPreferences.getCharsForContextInsensitiveModulesCompletion()
                && naturesUsed != null && naturesUsed.size() > 0) { //at least n characters required...

            int qlen = qual.length();
            boolean addAutoImport = AnalysisPreferences.doAutoImport(null);

            for (IPythonNature nature : naturesUsed) {
                fillCompletions(requestOffset, completions, qual, nature, qlen, addAutoImport, viewer);
            }

        }
        return completions;
    }

    private void fillCompletions(int requestOffset, ArrayList<ICompletionProposalHandle> completions, String qual,
            IPythonNature nature, int qlen, boolean addAutoImport, IScriptConsoleViewer viewer) {

        ICodeCompletionASTManager astManager = nature.getAstManager();
        if (astManager == null) {
            return;
        }

        IModulesManager modulesManager = astManager.getModulesManager();
        try {
            if (modulesManager == null) {
                nature.getProjectInterpreter(); //Just getting it here is likely to raise an error if it's not well configured.
            }
        } catch (PythonNatureWithoutProjectException e) {
            throw new RuntimeException(e);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }

        String lowerQual = qual.toLowerCase();
        final boolean useSubstringMatchInCodeCompletion = PyCodeCompletionPreferences
                .getUseSubstringMatchInCodeCompletion();
        Set<String> allModuleNames = PyCodeCompletionUtils.getModulesNamesToFilterOn(useSubstringMatchInCodeCompletion,
                modulesManager, qual);
        IFilter nameFilter = PyCodeCompletionUtils.getNameFilter(useSubstringMatchInCodeCompletion, qual);

        FastStringBuffer realImportRep = new FastStringBuffer();
        FastStringBuffer displayString = new FastStringBuffer();
        HashSet<String> alreadyFound = new HashSet<String>();

        ICompareContext compareContext = new CompareContext(nature);
        for (String name : allModuleNames) {

            FullRepIterable iterable = new FullRepIterable(name);
            for (String string : iterable) {
                //clear the buffer...
                realImportRep.clear();

                String[] strings = FullRepIterable.headAndTail(string);
                String importRep = strings[1];
                if (!nameFilter.acceptName(importRep)) {
                    continue;
                }

                displayString.clear();
                displayString.append(importRep);

                String packageName = strings[0];
                if (packageName.length() > 0) {
                    if (addAutoImport) {
                        realImportRep.append("from ");
                        realImportRep.append(packageName);
                        realImportRep.append(" ");
                    }
                    displayString.append(" - ");
                    displayString.append(packageName);
                }

                if (addAutoImport) {
                    realImportRep.append("import ");
                    realImportRep.append(strings[1]);
                }

                String found = displayString.toString();
                if (alreadyFound.contains(found)) {
                    continue;
                }
                alreadyFound.add(found);

                String displayAsStr = realImportRep.toString();
                ICompletionProposalHandle proposal = CompletionProposalFactory.get().createPyConsoleCompletion(
                        importRep,
                        requestOffset - qlen, qlen, realImportRep.length(),
                        IInfo.USE_PACKAGE_ICON, found, null, "",
                        displayAsStr.toLowerCase().equals(lowerQual) ? IPyCompletionProposal.PRIORITY_PACKAGES_EXACT
                                : IPyCompletionProposal.PRIORITY_PACKAGES,
                        displayAsStr, viewer,
                        compareContext);

                completions.add(proposal);
            }
        }
    }

    // Editor completions ----------------------------------------------------------------------------------------------

    private TokensOrProposalsList getThem(CompletionRequest request, ICompletionState state,
            boolean addAutoImport) throws MisconfigurationException {
        List<ICompletionProposalHandle> list = new ArrayList<>();
        if (request.isInCalltip) {
            return new TokensOrProposalsList(list);
        }

        if (request.qualifier.length() >= PyCodeCompletionPreferences
                .getCharsForContextInsensitiveModulesCompletion()) { //at least n characters required...

            ICodeCompletionASTManager astManager = request.nature.getAstManager();
            if (astManager == null) {
                return new TokensOrProposalsList(list);
            }
            String initialModule = request.resolveModule();

            IModulesManager projectModulesManager = astManager.getModulesManager();

            String lowerQual = request.qualifier.toLowerCase();
            final boolean useSubstringMatchInCodeCompletion = request.useSubstringMatchInCodeCompletion;
            IModulesManager[] managersInvolved = projectModulesManager.getManagersInvolved(true);
            for (int i = 0; i < managersInvolved.length; i++) {
                IModulesManager currentManager = managersInvolved[i];
                final Set<String> allModuleNames = PyCodeCompletionUtils.getModulesNamesToFilterOn(
                        useSubstringMatchInCodeCompletion, currentManager, request.qualifier);
                final IFilter nameFilter = PyCodeCompletionUtils.getNameFilter(useSubstringMatchInCodeCompletion,
                        request.qualifier);

                FastStringBuffer realImportRep = new FastStringBuffer();
                FastStringBuffer displayString = new FastStringBuffer();
                HashSet<String> importedNames = getImportedNames(state);

                for (String name : allModuleNames) {
                    if (name.equals(initialModule)) {
                        continue;
                    }

                    FullRepIterable iterable = new FullRepIterable(name);
                    for (String string : iterable) {
                        //clear the buffer...
                        realImportRep.clear();

                        String[] strings = FullRepIterable.headAndTail(string);
                        String importRep = strings[1];
                        if (!nameFilter.acceptName(importRep) || importedNames.contains(importRep)) {
                            continue;
                        }

                        displayString.clear();
                        displayString.append(importRep);

                        String packageName = strings[0];
                        if (packageName.length() > 0) {
                            if (addAutoImport) {
                                realImportRep.append("from ");
                                realImportRep.append(packageName);
                                realImportRep.append(" ");
                            }
                            displayString.append(" - ");
                            displayString.append(packageName);
                        }

                        if (addAutoImport) {
                            realImportRep.append("import ");
                            realImportRep.append(strings[1]);
                        }

                        String displayAsStr = displayString.toString();
                        ICompletionProposalHandle proposal = CompletionProposalFactory.get()
                                .createCtxInsensitiveImportComplProposal(importRep,
                                        request.documentOffset - request.qlen,
                                        request.qlen, realImportRep.length(), IInfo.USE_PACKAGE_ICON, displayAsStr,
                                        null, "",
                                        displayAsStr.toLowerCase().equals(lowerQual)
                                                ? IPyCompletionProposal.PRIORITY_PACKAGES_EXACT
                                                : IPyCompletionProposal.PRIORITY_PACKAGES,
                                        realImportRep.toString(), new CompareContext(currentManager.getNature()));

                        list.add(proposal);
                    }
                }
            }
        }
        return new TokensOrProposalsList(list);
    }

    private HashSet<String> getImportedNames(ICompletionState state) {
        TokensList tokenImportedModules = state.getTokenImportedModules();
        HashSet<String> importedNames = new HashSet<String>();
        if (tokenImportedModules != null) {
            for (IterTokenEntry entry : tokenImportedModules) {
                IToken token = entry.getToken();
                importedNames.add(token.getRepresentation());
            }
        }
        return importedNames;
    }

    @Override
    public TokensOrProposalsList getGlobalCompletions(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException {
        IAdaptable projectAdaptable = request.getNature() != null ? request.getNature().getProject() : null;
        return getThem(request, state, AnalysisPreferences.doAutoImport(projectAdaptable));
    }

    @Override
    public TokensList getCompletionsForMethodParameter(ICompletionState state, ILocalScope localScope,
            TokensList interfaceForLocal) {
        return new TokensList();
    }

    @Override
    public TokensOrProposalsList getStringGlobalCompletions(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException {
        return getThem(request, state, false);
    }

    @Override
    public TokensList getCompletionsForTokenWithUndefinedType(ICompletionState state, ILocalScope localScope,
            TokensList interfaceForLocal) {
        return new TokensList();
    }

    @Override
    public TokensList getCompletionsForType(ICompletionState state) {
        return new TokensList();
    }
}
