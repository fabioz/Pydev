package com.python.pydev.analysis.refactoring.quick_fixes;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.ast.codecompletion.ProposalsComparator.CompareContext;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInfo;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal.ICompareContext;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

public class UndefinedVariableQuickFixCreator {
    
    public static void createImportQuickProposalsFromMarkerSelectedText(PySelection ps, int offset,
            IPythonNature initialNature,
            List<ICompletionProposalHandle> props, ICodeCompletionASTManager astManager, int start, int end,
            boolean forceReparseOnApply)
            throws BadLocationException {
        ps.setSelection(start, end);
        String markerContents = ps.getSelectedText();
        String fullRep = ps.getFullRepAfterSelection();

        IModulesManager projectModulesManager = astManager.getModulesManager();
        IModulesManager[] managersInvolved = projectModulesManager.getManagersInvolved(true);
        boolean doIgnoreImportsStartingWithUnder = AnalysisPreferences.doIgnoreImportsStartingWithUnder();

        // Use a single buffer to create all the strings
        FastStringBuffer buffer = new FastStringBuffer();

        // Helper so that we don't add the same module multiple times.
        Set<Tuple<String, String>> mods = new HashSet<Tuple<String, String>>();

        for (IModulesManager iModulesManager : managersInvolved) {
            Set<String> allModules = iModulesManager.getAllModuleNames(false, markerContents.toLowerCase());

            //when an undefined variable is found, we can:
            // - add an auto import (if it is a class or a method or some global attribute)
            // - declare it as a local or global variable
            // - change its name to some other global or local (mistyped)
            // - create a method or class for it (if it is a call)

            //1. check if it is some module

            CompareContext compareContext = new CompareContext(iModulesManager.getNature());
            for (String completeName : allModules) {
                FullRepIterable iterable = new FullRepIterable(completeName);

                for (String mod : iterable) {

                    if (fullRep.startsWith(mod)) {

                        if (fullRep.length() == mod.length() //it does not only start with, but it is equal to it.
                                || (fullRep.length() > mod.length() && fullRep.charAt(mod.length()) == '.')) {
                            buffer.clear();
                            String realImportRep = buffer.append("import ").append(mod).toString();
                            buffer.clear();
                            String displayString = buffer.append("Import ").append(mod).toString();
                            addProp(props, realImportRep, displayString, IInfo.USE_PACKAGE_ICON, offset, mods,
                                    compareContext, forceReparseOnApply);
                        }
                    }

                    String[] strings = FullRepIterable.headAndTail(mod);
                    String packageName = strings[0];
                    String importRep = strings[1];

                    if (importRep.equals(markerContents)) {
                        if (packageName.length() > 0) {
                            buffer.clear();
                            String realImportRep = buffer.append("from ").append(packageName).append(" ")
                                    .append("import ")
                                    .append(strings[1]).toString();
                            buffer.clear();
                            String displayString = buffer.append("Import ").append(importRep).append(" (")
                                    .append(packageName).append(")").toString();
                            addProp(props, realImportRep, displayString, IInfo.USE_PACKAGE_ICON, offset, mods,
                                    compareContext, forceReparseOnApply);

                        } else {
                            buffer.clear();
                            String realImportRep = buffer.append("import ").append(strings[1]).toString();
                            buffer.clear();
                            String displayString = buffer.append("Import ").append(importRep).toString();
                            addProp(props, realImportRep, displayString, IInfo.USE_PACKAGE_ICON, offset, mods,
                                    compareContext, forceReparseOnApply);
                        }
                    }
                }
            }

        }
        //2. check if it is some global class or method
        List<AbstractAdditionalTokensInfo> additionalInfo;
        try {
            additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfo(initialNature);
        } catch (MisconfigurationException e) {
            return;
        }
        FastStringBuffer tempBuf = new FastStringBuffer();
        for (AbstractAdditionalTokensInfo info : additionalInfo) {
            Collection<IInfo> tokensEqualTo = info.getTokensEqualTo(markerContents,
                    AbstractAdditionalTokensInfo.TOP_LEVEL);
            for (IInfo found : tokensEqualTo) {
                //there always is a declaring module
                String name = found.getName();
                String declPackage = found.getDeclaringModuleName();
                String declPackageWithoutInit = declPackage;
                if (declPackageWithoutInit.endsWith(".__init__")) {
                    declPackageWithoutInit = declPackageWithoutInit.substring(0,
                            declPackageWithoutInit.length() - 9);
                }

                declPackageWithoutInit = AnalysisPreferences.removeImportsStartingWithUnderIfNeeded(
                        declPackageWithoutInit, tempBuf, doIgnoreImportsStartingWithUnder);

                buffer.clear();
                String importDeclaration = buffer.append("from ").append(declPackageWithoutInit).append(" import ")
                        .append(name).toString();

                buffer.clear();
                String displayImport = buffer.append("Import ").append(name).append(" (").append(declPackage)
                        .append(")").toString();

                addProp(props, importDeclaration, displayImport,
                        found.getType(),
                        offset, mods, new CompareContext(found.getNature()), forceReparseOnApply);
            }
        }
    }

    private static void addProp(List<ICompletionProposalHandle> props, String importDeclaration, String displayImport,
            int infoTypeForImage, int offset, Set<Tuple<String, String>> mods, ICompareContext compareContext,
            boolean forceReparseOnApply) {
        Tuple<String, String> tuple = new Tuple<String, String>(importDeclaration, displayImport);
        if (mods.contains(tuple)) {
            return;
        }

        mods.add(tuple);

        props.add(CompletionProposalFactory.get().createCtxInsensitiveImportComplProposalReparseOnApply("", offset, 0,
                0, infoTypeForImage,
                displayImport, null, importDeclaration, IPyCompletionProposal.PRIORITY_LOCALS, importDeclaration,
                compareContext, forceReparseOnApply));
    }

}
