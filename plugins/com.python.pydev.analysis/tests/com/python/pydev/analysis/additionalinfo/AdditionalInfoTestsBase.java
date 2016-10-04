/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 12, 2005
 *
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.PyCodeCompletionUtils;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.ModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisTestsBase;
import com.python.pydev.analysis.MarkerStub;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class AdditionalInfoTestsBase extends AnalysisTestsBase {

    protected IPyDevCompletionParticipant participant;
    protected boolean useOriginalRequestCompl = false;

    protected ArrayList<IToken> imports;

    @Override
    public ICompletionProposal[] requestCompl(File file, String strDoc, int documentOffset, int returned,
            String[] retCompl, PythonNature nature) throws Exception {
        if (useOriginalRequestCompl) {
            return super.requestCompl(file, strDoc, documentOffset, returned, retCompl, nature);
        }

        if (documentOffset == -1) {
            documentOffset = strDoc.length();
        }

        IDocument doc = new Document(strDoc);
        boolean useSubstringMatchInCodeCompletion = false;
        CompletionRequest request = new CompletionRequest(file, nature, doc, documentOffset, codeCompletion,
                useSubstringMatchInCodeCompletion);

        ICompletionState state = CompletionStateFactory.getEmptyCompletionState(nature, new CompletionCache());
        state.setTokenImportedModules(imports);
        List<Object> props = new ArrayList<Object>(participant.getGlobalCompletions(request, state));
        ICompletionProposal[] codeCompletionProposals = PyCodeCompletionUtils.onlyValid(props, request.qualifier,
                request.isInCalltip, useSubstringMatchInCodeCompletion, null);
        PyCodeCompletionUtils.sort(codeCompletionProposals, request.qualifier, null);

        for (int i = 0; i < retCompl.length; i++) {
            assertContains(retCompl[i], codeCompletionProposals);
        }

        if (returned > -1) {
            StringBuffer buffer = getAvailableAsStr(codeCompletionProposals);
            assertEquals("Expected " + returned + " received: " + codeCompletionProposals.length + "\n" + buffer,
                    returned, codeCompletionProposals.length);
        }
        return codeCompletionProposals;
    }

    /**
     * This method creates a marker stub
     *
     * @param start start char
     * @param end end char
     * @param type the marker type
     * @return the created stub
     */
    protected MarkerAnnotationAndPosition createMarkerStub(int start, int end, int type) {
        HashMap<String, Object> attrs = new HashMap<String, Object>();

        attrs.put(AnalysisRunner.PYDEV_ANALYSIS_TYPE, type);
        attrs.put(IMarker.CHAR_START, start);
        attrs.put(IMarker.CHAR_END, end);

        MarkerStub marker = new MarkerStub(attrs);
        return new MarkerAnnotationAndPosition(
                new MarkerAnnotation("org.eclipse.core.resources.problemmarker", marker), new Position(start, end
                        - start));
    }

    protected void addFooModule(final SimpleNode ast, File f) {
        String modName = "foo";
        PythonNature natureToAdd = nature;
        addModuleToNature(ast, modName, natureToAdd, f);
    }

    /**
     * @param ast the ast that defines the module
     * @param modName the module name
     * @param natureToAdd the nature where the module should be added
     */
    protected void addModuleToNature(final SimpleNode ast, String modName, PythonNature natureToAdd, File f) {
        //this is to add the info from the module that we just created...
        AbstractAdditionalDependencyInfo additionalInfo;
        try {
            additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(natureToAdd);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
        additionalInfo.addAstInfo(ast, new ModulesKey(modName, f), false);
        ModulesManager modulesManager = (ModulesManager) natureToAdd.getAstManager().getModulesManager();
        SourceModule mod = (SourceModule) AbstractModule.createModule(ast, f, modName, natureToAdd);
        modulesManager.doAddSingleModule(new ModulesKey(modName, f), mod);
    }

}
