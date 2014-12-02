/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 18/09/2005
 */
package com.python.pydev.analysis.builder;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.builder.VisitorMemo;
import org.python.pydev.core.IMiscConstants;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.parsing.ChangedParserInfoForObservers;
import org.python.pydev.shared_core.parsing.ErrorParserInfoForObservers;
import org.python.pydev.shared_core.parsing.IParserObserver;
import org.python.pydev.shared_core.parsing.IParserObserver3;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.AnalysisPreferences;
import com.python.pydev.analysis.IAnalysisPreferences;

/**
 * Observes changes to the parser and when OK, it'll ask for the analysis of the module reparsed.
 * 
 * @author Fabio
 */
public class AnalysisParserObserver implements IParserObserver, IParserObserver3 {

    /**
     * @author fabioz
     *
     */
    private final class AnalyzeLaterJob extends Job {
        private final IPythonNature nature;
        private ChangedParserInfoForObservers info;
        private SimpleNode root;
        private IFile fileAdapter;
        private boolean force;
        private int rescheduleTimes = 15;

        private AnalyzeLaterJob(String name, ChangedParserInfoForObservers info, SimpleNode root, IFile fileAdapter,
                boolean force, IPythonNature nature) {
            super(name);
            this.nature = nature;
            this.info = info;
            this.root = root;
            this.fileAdapter = fileAdapter;
            this.force = force;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            rescheduleTimes--;
            try {
                if (!nature.isOkToUse()) {
                    if (rescheduleTimes >= 0) {
                        this.schedule(200);
                    }
                } else {
                    analyze(info, root, fileAdapter, force, nature, false);
                }
            } catch (Throwable e) {
                Log.log(e);
            }
            return Status.OK_STATUS;
        }
    }

    public static final String ANALYSIS_PARSER_OBSERVER_FORCE = IMiscConstants.ANALYSIS_PARSER_OBSERVER_FORCE;

    public void parserChanged(final ChangedParserInfoForObservers info) {
        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
            System.out.println("AnalysisParserObserver: parserChanged");
        }
        final SimpleNode root = (SimpleNode) info.root;
        if (info.file == null) {
            return;
        }
        IFile fileAdapter = null;
        if (info.file instanceof IFile) {
            fileAdapter = (IFile) info.file;
        }

        if (fileAdapter == null) {
            fileAdapter = (IFile) info.file.getAdapter(IFile.class);
            if (fileAdapter == null) {
                return;
            }
        }
        boolean force = false;
        boolean forceAnalyzeInThisThread = false;
        if (info.argsToReparse != null && info.argsToReparse.length > 0) {
            if (info.argsToReparse[0] instanceof Tuple) {
                Tuple t = (Tuple) info.argsToReparse[0];
                if (t.o1 instanceof String && t.o2 instanceof Boolean) {
                    if (t.o1.equals(ANALYSIS_PARSER_OBSERVER_FORCE)) {
                        //if this message is passed, it will decide whether we will force the analysis or not
                        force = (Boolean) t.o2;
                    }
                    if (t.o1.equals(IMiscConstants.ANALYSIS_PARSER_OBSERVER_FORCE_IN_THIS_THREAD)) {
                        //if this message is passed, it will decide whether we will force the analysis or not
                        forceAnalyzeInThisThread = force = (Boolean) t.o2;
                    }
                }
            }
        }

        int whenAnalyze = new AnalysisPreferences(fileAdapter).getWhenAnalyze();
        if (whenAnalyze == IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE || force) {

            //create the module
            final IPythonNature nature = PythonNature.getPythonNature(fileAdapter);
            if (nature == null) {
                return;
            }

            //don't analyze it if we're still not 'all set'
            if (!nature.isOkToUse()) {
                Job job = new AnalyzeLaterJob("Analyze later", info, root, fileAdapter, force, nature);
                job.schedule(100);
                return;
            }

            analyze(info, root, fileAdapter, force, nature, forceAnalyzeInThisThread);
        }
    }

    private void analyze(ChangedParserInfoForObservers info, SimpleNode root, IFile fileAdapter, boolean force,
            IPythonNature nature, boolean forceAnalyzeInThisThread) {
        if (!nature.startRequests()) {
            return;
        }
        IModule module;
        try {
            //we visit external because we must index them
            String moduleName = nature.resolveModuleOnlyInProjectSources(fileAdapter, true);
            if (moduleName == null) {
                AnalysisRunner.deleteMarkers(fileAdapter);
                return; // we only analyze resources that are in the pythonpath
            }

            String file = fileAdapter.getRawLocation().toOSString();
            module = AbstractModule.createModule(root, new File(file), moduleName);

        } catch (Exception e) {
            Log.log(e); //Not much we can do about it.
            return;
        } finally {
            nature.endRequests();
        }

        //visit it
        AnalysisBuilderVisitor visitor = new AnalysisBuilderVisitor();
        visitor.memo = new VisitorMemo();
        visitor.memo.put(PyDevBuilderVisitor.IS_FULL_BUILD, false);
        visitor.memo.put(PyDevBuilderVisitor.DOCUMENT_TIME, info.documentMillisTime);
        visitor.visitingWillStart(new NullProgressMonitor(), false, null);
        try {
            visitor.doVisitChangedResource(nature, fileAdapter, info.doc, null, module, new NullProgressMonitor(),
                    force, AnalysisBuilderRunnable.ANALYSIS_CAUSE_PARSER, info.documentMillisTime,
                    forceAnalyzeInThisThread);
        } finally {
            visitor.visitingEnded(new NullProgressMonitor());
        }

    }

    public void parserChanged(ISimpleNode root, IAdaptable resource, IDocument doc, long docModificationStamp) {
        throw new RuntimeException("As it uses IParserObserver2, this interface should not be asked for.");
    }

    public void parserError(Throwable error, IAdaptable file, IDocument doc) {
        //ignore errors...
    }

    public void parserError(ErrorParserInfoForObservers info) {
        //ignore
    }

}
