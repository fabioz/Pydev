/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis.ctrl_1;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.ProposalsComparator.CompareContext;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal.ICompareContext;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;
import com.python.pydev.analysis.builder.AnalysisParserObserver;
import com.python.pydev.analysis.builder.AnalysisRunner;
import com.python.pydev.analysis.ui.AutoImportsPreferencesPage;

/**
 * Class that'll create proposals for fixing an undefined variable found.
 *
 * @author Fabio
 */
public class UndefinedVariableFixParticipant implements IAnalysisMarkersParticipant {

    /**
     * Defines whether a reparse should be forced after applying the completion.
     */
    private boolean forceReparseOnApply;

    public UndefinedVariableFixParticipant() {
        this(true);
    }

    public UndefinedVariableFixParticipant(boolean forceReparseOnApply) {
        this.forceReparseOnApply = forceReparseOnApply;
    }

    /**
     * @see IAnalysisMarkersParticipant#addProps(MarkerAnnotation, IAnalysisPreferences, String, PySelection, int, IPythonNature,
     * PyEdit, List)
     *
     */
    @Override
    public void addProps(MarkerAnnotationAndPosition markerAnnotation, IAnalysisPreferences analysisPreferences,
            String line, PySelection ps, int offset, IPythonNature initialNature, PyEdit edit,
            List<ICompletionProposal> props)
            throws BadLocationException, CoreException {
        IMarker marker = markerAnnotation.markerAnnotation.getMarker();
        Integer id = (Integer) marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
        if (id != IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE) {
            return;
        }
        if (initialNature == null) {
            return;
        }
        ICodeCompletionASTManager astManager = initialNature.getAstManager();
        if (astManager == null) {
            return;
        }

        if (markerAnnotation.position == null) {
            return;
        }
        int start = markerAnnotation.position.offset;
        int end = start + markerAnnotation.position.length;
        ps.setSelection(start, end);
        String markerContents = ps.getSelectedText();
        String fullRep = ps.getFullRepAfterSelection();

        ImageCache imageCache = PydevPlugin.getImageCache();
        Image packageImage = null;
        if (imageCache != null) { //making tests
            packageImage = imageCache.get(UIConstants.COMPLETION_PACKAGE_ICON);
        }
        IModulesManager projectModulesManager = astManager.getModulesManager();
        IModulesManager[] managersInvolved = projectModulesManager.getManagersInvolved(true);
        boolean doIgnoreImportsStartingWithUnder = AutoImportsPreferencesPage.doIgnoreImportsStartingWithUnder();

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
                            addProp(props, realImportRep, displayString, packageImage, offset, mods,
                                    compareContext);
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
                            addProp(props, realImportRep, displayString, packageImage, offset, mods,
                                    compareContext);

                        } else {
                            buffer.clear();
                            String realImportRep = buffer.append("import ").append(strings[1]).toString();
                            buffer.clear();
                            String displayString = buffer.append("Import ").append(importRep).toString();
                            addProp(props, realImportRep, displayString, packageImage, offset, mods,
                                    compareContext);
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

                declPackageWithoutInit = AutoImportsPreferencesPage.removeImportsStartingWithUnderIfNeeded(
                        declPackageWithoutInit, tempBuf, doIgnoreImportsStartingWithUnder);

                buffer.clear();
                String importDeclaration = buffer.append("from ").append(declPackageWithoutInit).append(" import ")
                        .append(name).toString();

                buffer.clear();
                String displayImport = buffer.append("Import ").append(name).append(" (").append(declPackage)
                        .append(")").toString();

                addProp(props, importDeclaration, displayImport,
                        AnalysisPlugin.getImageForAutoImportTypeInfo(found),
                        offset, mods, new CompareContext(found.getNature()));
            }
        }
    }

    private void addProp(List<ICompletionProposal> props, String importDeclaration, String displayImport,
            Image importImage, int offset, Set<Tuple<String, String>> mods, ICompareContext compareContext) {
        Tuple<String, String> tuple = new Tuple<String, String>(importDeclaration, displayImport);
        if (mods.contains(tuple)) {
            return;
        }

        mods.add(tuple);

        props.add(new CtxInsensitiveImportComplProposal("", offset, 0, 0, importImage, displayImport, null,
                importDeclaration,
                IPyCompletionProposal.PRIORITY_LOCALS, importDeclaration, compareContext) {

            @Override
            public void selected(ITextViewer viewer, boolean smartToggle) {
                //Overridden to do nothing (i.e.: don't leave yellow when ctrl is pressed).
            }

            @Override
            public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
                if ((stateMask & SWT.SHIFT) != 0) {
                    this.setAddLocalImport(true);
                }
                super.apply(viewer, trigger, stateMask, offset);
                if (forceReparseOnApply) {
                    //and after applying it, let's request a reanalysis
                    if (viewer instanceof PySourceViewer) {
                        PySourceViewer sourceViewer = (PySourceViewer) viewer;
                        PyEdit edit = sourceViewer.getEdit();
                        if (edit != null) {
                            edit.getParser().forceReparse(
                                    new Tuple<String, Boolean>(AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE,
                                            true));
                        }
                    }
                }
            }

        });
    }

}
