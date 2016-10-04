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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant2;
import org.python.pydev.editor.codecompletion.ProposalsComparator.CompareContext;
import org.python.pydev.editor.codecompletion.PyCodeCompletionImages;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.PyCodeCompletionUtils;
import org.python.pydev.editor.codecompletion.PyCodeCompletionUtils.IFilter;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleViewer;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal.ICompareContext;

import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.ui.AutoImportsPreferencesPage;
import com.python.pydev.codecompletion.ctxinsensitive.PyConsoleCompletion;
import com.python.pydev.codecompletion.ui.CodeCompletionPreferencesPage;

/**
 * Gathers completions from the modules available (for the editor or for the console).
 *
 * @author Fabio
 */
public class ImportsCompletionParticipant implements IPyDevCompletionParticipant, IPyDevCompletionParticipant2 {

    // Console completions ---------------------------------------------------------------------------------------------

    @Override
    public Collection<ICompletionProposal> computeConsoleCompletions(ActivationTokenAndQual tokenAndQual,
            Set<IPythonNature> naturesUsed, IScriptConsoleViewer viewer, int requestOffset) {
        ArrayList<ICompletionProposal> completions = new ArrayList<ICompletionProposal>();

        if (tokenAndQual.activationToken != null && tokenAndQual.activationToken.length() > 0) {
            //we only want
            return completions;
        }

        String qual = tokenAndQual.qualifier;
        if (qual.length() >= CodeCompletionPreferencesPage.getCharsForContextInsensitiveModulesCompletion()
                && naturesUsed != null && naturesUsed.size() > 0) { //at least n characters required...

            int qlen = qual.length();
            boolean addAutoImport = AutoImportsPreferencesPage.doAutoImport();

            for (IPythonNature nature : naturesUsed) {
                fillCompletions(requestOffset, completions, qual, nature, qlen, addAutoImport, viewer);
            }

        }
        return completions;
    }

    private void fillCompletions(int requestOffset, ArrayList<ICompletionProposal> completions, String qual,
            IPythonNature nature, int qlen, boolean addAutoImport, IScriptConsoleViewer viewer) {

        ICodeCompletionASTManager astManager = nature.getAstManager();
        if (astManager == null) {
            return;
        }

        Image img = PyCodeCompletionImages.getImageForType(IToken.TYPE_PACKAGE);

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
        final boolean useSubstringMatchInCodeCompletion = PyCodeCompletionPreferencesPage
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
                PyConsoleCompletion proposal = new PyConsoleCompletion(importRep, requestOffset - qlen, qlen,
                        realImportRep.length(), img, found, (IContextInformation) null, "",
                        displayAsStr.toLowerCase().equals(lowerQual) ? IPyCompletionProposal.PRIORITY_PACKAGES_EXACT
                                : IPyCompletionProposal.PRIORITY_PACKAGES,
                        displayAsStr, viewer, compareContext);

                completions.add(proposal);
            }
        }
    }

    // Editor completions ----------------------------------------------------------------------------------------------

    private Collection<CtxInsensitiveImportComplProposal> getThem(CompletionRequest request, ICompletionState state,
            boolean addAutoImport) throws MisconfigurationException {
        ArrayList<CtxInsensitiveImportComplProposal> list = new ArrayList<CtxInsensitiveImportComplProposal>();
        if (request.isInCalltip) {
            return list;
        }

        if (request.qualifier.length() >= CodeCompletionPreferencesPage
                .getCharsForContextInsensitiveModulesCompletion()) { //at least n characters required...

            ICodeCompletionASTManager astManager = request.nature.getAstManager();
            if (astManager == null) {
                return list;
            }
            String initialModule = request.resolveModule();

            Image img = PyCodeCompletionImages.getImageForType(IToken.TYPE_PACKAGE);

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
                        CtxInsensitiveImportComplProposal proposal = new CtxInsensitiveImportComplProposal(
                                importRep,
                                request.documentOffset - request.qlen,
                                request.qlen,
                                realImportRep.length(),
                                img,
                                displayAsStr,
                                (IContextInformation) null,
                                "",
                                displayAsStr.toLowerCase().equals(lowerQual)
                                        ? IPyCompletionProposal.PRIORITY_PACKAGES_EXACT
                                        : IPyCompletionProposal.PRIORITY_PACKAGES,
                                realImportRep.toString(),
                                new CompareContext(currentManager.getNature()));

                        list.add(proposal);
                    }
                }
            }
        }
        return list;
    }

    private HashSet<String> getImportedNames(ICompletionState state) {
        List<IToken> tokenImportedModules = state.getTokenImportedModules();
        HashSet<String> importedNames = new HashSet<String>();
        if (tokenImportedModules != null) {
            for (IToken token : tokenImportedModules) {
                importedNames.add(token.getRepresentation());
            }
        }
        return importedNames;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection getGlobalCompletions(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException {
        return getThem(request, state, AutoImportsPreferencesPage.doAutoImport());
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection getCompletionsForMethodParameter(ICompletionState state, ILocalScope localScope,
            Collection<IToken> interfaceForLocal) {
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection getStringGlobalCompletions(CompletionRequest request, ICompletionState state)
            throws MisconfigurationException {
        return getThem(request, state, false);
    }

    @Override
    public Collection<Object> getArgsCompletion(ICompletionState state, ILocalScope localScope,
            Collection<IToken> interfaceForLocal) {
        throw new RuntimeException("Deprecated");
    }

    @Override
    public Collection<IToken> getCompletionsForTokenWithUndefinedType(ICompletionState state, ILocalScope localScope,
            Collection<IToken> interfaceForLocal) {
        return Collections.emptyList();
    }

    @Override
    public Collection<IToken> getCompletionsForType(ICompletionState state) {
        return null;
    }
}
