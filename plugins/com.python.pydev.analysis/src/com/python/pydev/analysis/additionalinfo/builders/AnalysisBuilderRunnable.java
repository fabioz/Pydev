/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 6, 2006
 */
package com.python.pydev.analysis.additionalinfo.builders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.analysis.IAnalysisPreferences;
import org.python.pydev.ast.analysis.messages.IMessage;
import org.python.pydev.ast.builder.PyDevBuilderVisitor;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.core.CheckAnalysisErrors;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.editor.OpenEditors;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.logging.DebugSettings;
import org.python.pydev.parser.preferences.PyDevBuilderPreferences;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.resources.DocumentChanged;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.OccurrencesAnalyzer;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.external.IExternalCodeAnalysisVisitor;
import com.python.pydev.analysis.flake8.Flake8Visitor;
import com.python.pydev.analysis.flake8.Flake8VisitorFactory;
import com.python.pydev.analysis.flake8.OnlyRemoveMarkersFlake8Visitor;
import com.python.pydev.analysis.mypy.MypyVisitor;
import com.python.pydev.analysis.mypy.MypyVisitorFactory;
import com.python.pydev.analysis.mypy.OnlyRemoveMarkersMypyVisitor;
import com.python.pydev.analysis.pylint.OnlyRemoveMarkersPyLintVisitor;
import com.python.pydev.analysis.pylint.PyLintVisitor;
import com.python.pydev.analysis.pylint.PyLintVisitorFactory;
import com.python.pydev.analysis.ruff.OnlyRemoveMarkersRuffVisitor;
import com.python.pydev.analysis.ruff.RuffVisitor;
import com.python.pydev.analysis.ruff.RuffVisitorFactory;

/**
 * This class is used to do analysis on a thread, so that if an analysis is asked for some analysis that
 * is already in progress, that analysis will be stopped and this one will begin.
 *
 * @author Fabio
 */
public class AnalysisBuilderRunnable extends AbstractAnalysisBuilderRunnable {

    /**
     * These are the callbacks called whenever there's a run to be done in this class.
     */
    public static final List<ICallback<Object, IResource>> analysisBuilderListeners = new ArrayList<ICallback<Object, IResource>>();

    // -------------------------------------------------------------------------------------------- ATTRIBUTES

    private IDocument document;
    private IResource resource;
    private ICallback<IModule, Integer> module;
    private int moduleRequest;
    private IExternalCodeAnalysisVisitor pyLintVisitor;
    private IExternalCodeAnalysisVisitor mypyVisitor;
    private IExternalCodeAnalysisVisitor flake8Visitor;
    private IExternalCodeAnalysisVisitor ruffVisitor;

    private boolean onlyRecreateCtxInsensitiveInfo;

    private IExternalCodeAnalysisVisitor[] allVisitors;

    // ---------------------------------------------------------------------------------------- END ATTRIBUTES

    /**
     * Checks if some resource is hierarchically derived (if any parent is derived, it's also derived).
     */
    private static boolean isHierarchicallyDerived(IResource curr) {
        return curr.isDerived(IResource.CHECK_ANCESTORS);
    }

    /**
     * @param oldAnalysisBuilderThread This is an existing runnable that was already analyzing things... we must wait for it
     * to finish to start it again.
     * @param externalVisitors
     *
     * @param module: this is a callback that'll be called with a boolean that should return the IModule to be used in the
     * analysis.
     * The parameter is FULL_MODULE or DEFINITIONS_MODULE
     */
    /*Default*/ AnalysisBuilderRunnable(IDocument document, IResource resource, ICallback<IModule, Integer> module,
            boolean isFullBuild, String moduleName, boolean forceAnalysis, int analysisCause,
            IAnalysisBuilderRunnable oldAnalysisBuilderThread, IPythonNature nature, long documentTime,
            KeyForAnalysisRunnable key, long resourceModificationStamp,
            List<IExternalCodeAnalysisVisitor> externalVisitors) {
        super(isFullBuild, moduleName, forceAnalysis, analysisCause, oldAnalysisBuilderThread, nature, documentTime,
                key, resourceModificationStamp);

        if (resource == null) {
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, "Unexpected null resource for: " + moduleName);
            return;
        }
        this.document = document;
        this.resource = resource;
        this.module = module;

        if (externalVisitors.size() > 0) {
            this.allVisitors = externalVisitors.toArray(new IExternalCodeAnalysisVisitor[0]);
            for (IExternalCodeAnalysisVisitor visitor : allVisitors) {
                if (visitor instanceof OnlyRemoveMarkersPyLintVisitor || visitor instanceof PyLintVisitor) {
                    this.pyLintVisitor = visitor;
                } else if (visitor instanceof OnlyRemoveMarkersMypyVisitor || visitor instanceof MypyVisitor) {
                    this.mypyVisitor = visitor;
                } else if (visitor instanceof OnlyRemoveMarkersFlake8Visitor || visitor instanceof Flake8Visitor) {
                    this.flake8Visitor = visitor;
                } else if (visitor instanceof OnlyRemoveMarkersRuffVisitor || visitor instanceof RuffVisitor) {
                    this.ruffVisitor = visitor;
                }
            }
            if (pyLintVisitor == null || mypyVisitor == null || flake8Visitor == null || ruffVisitor == null) {
                throw new AssertionError("All visitor types must be passed.");
            }
        } else {
            this.pyLintVisitor = PyLintVisitorFactory.create(resource, document, module, internalCancelMonitor);
            this.mypyVisitor = MypyVisitorFactory.create(resource, document, module, internalCancelMonitor);
            this.flake8Visitor = Flake8VisitorFactory.create(resource, document, module, internalCancelMonitor);
            this.ruffVisitor = RuffVisitorFactory.create(resource, document, module, internalCancelMonitor);
            this.allVisitors = new IExternalCodeAnalysisVisitor[] { this.pyLintVisitor, this.mypyVisitor,
                    this.flake8Visitor, this.ruffVisitor };
        }

        // Important: we can only update the index if it was a builder... if it was the parser,
        // we can't update it otherwise we could end up with data that's not saved in the index.
        boolean updateIndex = analysisCause == ANALYSIS_CAUSE_BUILDER;

        // Previously we did this in a thread, but updating the indexes in a thread made things too
        // unreliable for the index (it was not uncommon for it to become unsynchronized as we can't
        // guarantee the order of operations).
        // So, this process is now synchronous (just the code-analysis is done in a thread now).
        try {
            onlyRecreateCtxInsensitiveInfo = !forceAnalysis && analysisCause == ANALYSIS_CAUSE_BUILDER
                    && PyDevBuilderPreferences.getAnalyzeOnlyActiveEditor();

            if (!onlyRecreateCtxInsensitiveInfo) {
                //if not a source folder, we'll just want to recreate the context insensitive information
                if (!nature.isResourceInPythonpathProjectSources(resource, false)) {
                    onlyRecreateCtxInsensitiveInfo = true;
                }
            }

            AbstractAdditionalTokensInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);

            if (info == null) {
                Log.log("Unable to get additional info for: " + resource + " -- " + moduleName);
                return;
            }

            //remove dependency information (and anything else that was already generated)
            if (!isFullBuild && updateIndex) {
                //if it is a full build, that info is already removed
                AnalysisBuilderRunnableForRemove.removeInfoForModule(moduleName, nature, isFullBuild);
            }

            if (onlyRecreateCtxInsensitiveInfo) {
                moduleRequest = DEFINITIONS_MODULE;
            } else {
                moduleRequest = FULL_MODULE;
            }

            //recreate the ctx insensitive info
            if (updateIndex) {
                recreateCtxInsensitiveInfo(info, (SourceModule) this.module.call(moduleRequest), nature, resource);
            }

        } catch (MisconfigurationException | CoreException e) {
            Log.log(e);
        }

    }

    @Override
    protected void dispose() {
        super.dispose();
        this.document = null;
        this.resource = null;
        this.module = null;
    }

    @Override
    protected void doAnalysis() {

        if (!nature.startRequests()) {
            return;
        }
        try {

            if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                        "doAnalysis() - " + moduleName + " " + this.getAnalysisCauseStr());
            }
            //if the resource is not open, there's not much we can do...
            final IResource r = resource;
            if (r == null) {
                org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                        "Finished analysis -- resource null -- " + moduleName);
                return;
            }

            if (!r.getProject().isOpen()) {
                org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                        "Finished analysis -- project closed -- " + moduleName);
                return;
            }

            AnalysisRunner runner = new AnalysisRunner();
            checkStop();

            IAnalysisPreferences analysisPreferences = new AnalysisPreferences(r);

            boolean makeAnalysis = runner.canDoAnalysis(document) && PyDevBuilderVisitor.isInPythonPath(r) && //just get problems in resources that are in the pythonpath
                    analysisPreferences.makeCodeAnalysis();

            boolean anotherVisitorRequiresAnalysis = false;
            for (IExternalCodeAnalysisVisitor visitor : allVisitors) {
                anotherVisitorRequiresAnalysis |= visitor.getRequiresAnalysis();
            }

            if (!makeAnalysis) {
                //let's see if we should do code analysis
                if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                    org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                            "Skipping: !makeAnalysis -- " + moduleName);
                }
                if (!anotherVisitorRequiresAnalysis) {
                    AnalysisRunner.deleteMarkers(r);
                    return;
                } else {
                    // Only delete pydev markers (others will be deleted by the respective visitors later on).
                    boolean onlyPydevAnalysisMarkers = true;
                    AnalysisRunner.deleteMarkers(r, onlyPydevAnalysisMarkers);
                }
            }

            if (makeAnalysis && onlyRecreateCtxInsensitiveInfo) {
                if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                    org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                            "Skipping: !forceAnalysis && analysisCause == ANALYSIS_CAUSE_BUILDER && "
                                    + "PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor() -- " + moduleName);
                }
                return;
            }

            if (nature == null) {
                Log.log("Finished analysis: null nature -- " + moduleName);
                return;
            }
            AbstractAdditionalTokensInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);

            if (info == null) {
                Log.log("Unable to get additional info for: " + r + " -- " + moduleName);
                return;
            }

            if (makeAnalysis && DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                        "makeAnalysis:" + makeAnalysis + " " + "analysisCause: " + getAnalysisCauseStr()
                                + " -- " + moduleName);
            }

            checkStop();

            if (isHierarchicallyDerived(r)) {
                if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                    org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                            "Resource marked as derived not analyzed: " + r + " -- " + moduleName);
                }
                //We don't want to check derived resources (but we want to remove any analysis messages that
                //might be already there)
                if (r != null) {
                    AnalysisRunner.deleteMarkers(r);
                }
                return;
            }

            // Currently, the PyLint/Mypy visitor can only analyze the contents saved, so, if the contents on the doc
            // changed in the meanwhile, skip doing this visit.
            // Maybe we can improve that when https://github.com/PyCQA/pylint/pull/1189 is done.
            if (!DocumentChanged.hasDocumentChanged(resource, document)) {
                for (IExternalCodeAnalysisVisitor visitor : allVisitors) {
                    visitor.startVisit();
                }
            } else {
                for (IExternalCodeAnalysisVisitor visitor : allVisitors) {
                    visitor.deleteMarkers();
                }
                if (!makeAnalysis) {
                    return;
                }
            }

            List<MarkerInfo> markersFromCodeAnalysis = null;
            if (makeAnalysis) {
                OccurrencesAnalyzer analyzer = new OccurrencesAnalyzer();
                checkStop();
                SourceModule module = (SourceModule) this.module.call(moduleRequest);
                IMessage[] messages = analyzer.analyzeDocument(nature, module, analysisPreferences, document,
                        this.internalCancelMonitor, DefaultIndentPrefs.get(this.resource));

                checkStop();
                if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                    org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                            "Adding markers for module: " + moduleName);
                    //for (IMessage message : messages) {
                    //    Log.toLogFile(this, message.toString());
                    //}
                }

                //last chance to stop...
                checkStop();

                //don't stop after setting to add / remove the markers
                if (r != null) {
                    boolean analyzeOnlyActiveEditor = PyDevBuilderPreferences.getAnalyzeOnlyActiveEditor();
                    if (forceAnalysis
                            || !analyzeOnlyActiveEditor
                            || (analyzeOnlyActiveEditor
                                    && (!PyDevBuilderPreferences.getRemoveErrorsWhenEditorIsClosed() || OpenEditors
                                            .isEditorOpenForResource(r)))) {
                        markersFromCodeAnalysis = runner.setMarkers(r, document, messages, this.internalCancelMonitor);
                    } else {
                        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this,
                                    "Skipped adding markers for module: " + moduleName
                                            + " (editor not opened).");
                        }
                    }
                }

            }
            //if there are callbacks registered, call them if we still didn't return (mostly for tests)
            for (ICallback<Object, IResource> callback : analysisBuilderListeners) {
                try {
                    callback.call(r);
                } catch (Exception e) {
                    Log.log(e);
                }
            }

            checkStop();
            for (IExternalCodeAnalysisVisitor visitor : allVisitors) {
                visitor.join();
            }

            checkStop();
            if (r != null) {
                for (IExternalCodeAnalysisVisitor visitor : allVisitors) {
                    String problemMarker = visitor.getProblemMarkerId();
                    String messageId = visitor.getMessageId();

                    List<MarkerInfo> markersFromVisitor = visitor.getMarkers(resource);
                    if (markersFromVisitor != null && markersFromVisitor.size() > 0) {

                        Map<Integer, List<MarkerInfo>> lineToMarkerInfo = new HashMap<>();
                        if (markersFromCodeAnalysis != null) {
                            for (MarkerInfo codeAnalysisMarkerInfo : markersFromCodeAnalysis) {
                                List<MarkerInfo> list = lineToMarkerInfo.get(codeAnalysisMarkerInfo.lineStart);
                                if (list == null) {
                                    list = new ArrayList<>(2);
                                    lineToMarkerInfo.put(codeAnalysisMarkerInfo.lineStart, list);
                                }
                                list.add(codeAnalysisMarkerInfo);
                            }
                        }

                        if (visitor == pyLintVisitor) {
                            // I.e.: if the error is already generated in the PyDev code-analysis, skip the same error on PyLint
                            // (there's no real point in putting an error twice).
                            for (Iterator<MarkerInfo> visitorMarkerInfoIterator = markersFromVisitor
                                    .iterator(); visitorMarkerInfoIterator.hasNext();) {
                                MarkerInfo visitorMarkerInfo = visitorMarkerInfoIterator.next();
                                List<MarkerInfo> codeAnalysisMarkers = lineToMarkerInfo
                                        .get(visitorMarkerInfo.lineStart);
                                if (codeAnalysisMarkers != null && codeAnalysisMarkers.size() > 0) {
                                    for (MarkerInfo codeAnalysisMarker : codeAnalysisMarkers) {
                                        if (codeAnalysisMarker.severity < IMarker.SEVERITY_INFO) {
                                            // Don't consider if it shouldn't be shown.
                                            continue;
                                        }
                                        Map<String, Object> additionalInfo = codeAnalysisMarker.additionalInfo;
                                        if (additionalInfo != null) {
                                            Object analysisType = additionalInfo
                                                    .get(AnalysisRunner.PYDEV_ANALYSIS_TYPE);
                                            if (analysisType != null && analysisType instanceof Integer) {
                                                String pyLintMessageId = CheckAnalysisErrors
                                                        .getPyLintMessageIdForPyDevAnalysisType((int) analysisType);
                                                if (pyLintMessageId != null
                                                        && pyLintMessageId.equals(visitorMarkerInfo.additionalInfo
                                                                .get(messageId))) {
                                                    visitorMarkerInfoIterator.remove();
                                                    break; // Stop the for (we've already removed it).
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        PyMarkerUtils.replaceMarkers(markersFromVisitor, resource, problemMarker,
                                true, this.internalCancelMonitor);
                    } else {
                        visitor.deleteMarkers();
                    }
                }
            }

        } catch (OperationCanceledException e) {
            //ok, ignore it
            logOperationCancelled();
        } catch (Exception e) {
            Log.log(e);
        } finally {
            try {
                nature.endRequests();
            } catch (Throwable e) {
                Log.log("Error when analyzing: " + moduleName, e);
            }
            try {
                AnalysisBuilderRunnableFactory.removeFromThreads(key, this);
            } catch (Throwable e) {
                Log.log(e);
            }

            dispose();
        }
    }

    /**
     * @return false if there's no modification among the current version of the file and the last version analyzed.
     */
    private void recreateCtxInsensitiveInfo(AbstractAdditionalTokensInfo info, SourceModule sourceModule,
            IPythonNature nature, IResource r) {

        //info.removeInfoFromModule(sourceModule.getName()); -- does not remove info from the module because this
        //should be already done once it gets here (the AnalysisBuilder, that also makes dependency info
        //should take care of this).
        boolean generateDelta;
        if (isFullBuild) {
            generateDelta = false;
        } else {
            generateDelta = true;
        }
        info.addAstInfo(sourceModule.getAst(), sourceModule.getModulesKey(), generateDelta);
    }

}
