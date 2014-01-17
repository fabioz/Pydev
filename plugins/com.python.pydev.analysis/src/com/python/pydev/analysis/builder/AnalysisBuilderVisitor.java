/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.builder;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.concurrency.RunnableAsJobsPoolThread;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PyCodeCompletionVisitor;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.fastparser.FastDefinitionsParser;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.callbacks.ICallback0;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

public class AnalysisBuilderVisitor extends PyDevBuilderVisitor {

    @Override
    protected int getPriority() {
        return PyCodeCompletionVisitor.PRIORITY_CODE_COMPLETION + 1; //just after the code-completion priority
    }

    @Override
    public void visitChangedResource(final IResource resource, final ICallback0<IDocument> document,
            final IProgressMonitor monitor) {
        visitChangedResource(resource, document, monitor, false);
    }

    public void visitChangedResource(final IResource resource, final ICallback0<IDocument> document,
            final IProgressMonitor monitor, boolean forceAnalysis) {
        //we may need to 'force' the analysis when a module is renamed, because the first message we receive is
        //a 'delete' and after that an 'add' -- which is later mapped to this method, so, if we don't have info
        //on the module we should analyze it because it is 'probably' a rename.
        final PythonNature nature = getPythonNature(resource);
        if (nature == null) {
            return;
        }

        //Put things from the memo to final variables as we might need them later on and we cannot get them from
        //the memo later.
        final String moduleName;
        final SourceModule[] module = new SourceModule[] { null };
        final IDocument doc;
        doc = document.call();
        if (doc == null) {
            return;
        }

        try {
            moduleName = getModuleName(resource, nature);
        } catch (MisconfigurationException e) {
            Log.log(e);
            return;
        }

        //depending on the level of analysis we have to do, we'll decide whether we want
        //to make the full parse (slower) or the definitions parse (faster but only with info
        //related to the definitions)
        ICallback<IModule, Integer> moduleCallback = new ICallback<IModule, Integer>() {

            public IModule call(Integer arg) {

                //Note: we cannot get anything from the memo at this point because it'll be called later on from a thread
                //and the memo might have changed already (E.g: moduleName and module)

                if (arg == IAnalysisBuilderRunnable.FULL_MODULE) {

                    if (module[0] != null) {
                        return module[0];
                    } else {
                        try {
                            module[0] = getSourceModule(resource, doc, nature);
                        } catch (MisconfigurationException e1) {
                            throw new RuntimeException(e1);
                        }
                        if (module[0] != null) {
                            return module[0];
                        }

                        try {
                            module[0] = createSoureModule(resource, doc, moduleName);
                        } catch (MisconfigurationException e) {
                            throw new RuntimeException(e);
                        }
                        return module[0];
                    }

                } else if (arg == IAnalysisBuilderRunnable.DEFINITIONS_MODULE) {
                    if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                        Log.toLogFile(this, "PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()");
                    }
                    IFile f = (IFile) resource;
                    String file = f.getRawLocation().toOSString();
                    return new SourceModule(moduleName, new File(file), FastDefinitionsParser.parse(doc.get(),
                            moduleName), null);

                } else {
                    throw new RuntimeException("Unexpected parameter: " + arg);
                }
            }
        };

        long documentTime = this.getDocumentTime();
        if (documentTime == -1) {
            Log.log("Warning: The document time in the visitor is -1. Changing for current time.");
            documentTime = System.currentTimeMillis();
        }
        doVisitChangedResource(nature, resource, doc, moduleCallback, null, monitor, forceAnalysis,
                AnalysisBuilderRunnable.ANALYSIS_CAUSE_BUILDER, documentTime, false);
    }

    /**
     * here we have to detect errors / warnings from the code analysis
     * Either the module callback or the module must be set.
     * @param forceAnalyzeInThisThread 
     */
    public void doVisitChangedResource(IPythonNature nature, IResource resource, IDocument document,
            ICallback<IModule, Integer> moduleCallback, final IModule module, IProgressMonitor monitor,
            boolean forceAnalysis, int analysisCause, long documentTime, boolean forceAnalyzeInThisThread) {
        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
            if (analysisCause == AnalysisBuilderRunnable.ANALYSIS_CAUSE_BUILDER) {
                System.out.println("doVisitChangedResource: BUILDER -- " + documentTime);
            } else {
                System.out.println("doVisitChangedResource: PARSER -- " + documentTime);
            }
        }

        if (module != null) {
            if (moduleCallback != null) {
                Log.log("Only the module or the moduleCallback must be specified for: " + resource);
                return;
            }
            setModuleInCache(resource, module);

            moduleCallback = new ICallback<IModule, Integer>() {

                public IModule call(Integer arg) {
                    return module;
                }
            };
        } else {
            //don't set module in the cache if we only have the callback
            //moduleCallback is already defined
            if (moduleCallback == null) {
                Log.log("Either the module or the moduleCallback must be specified for: " + resource);
                return;
            }
        }

        String moduleName;
        try {
            moduleName = getModuleName(resource, nature);
        } catch (MisconfigurationException e) {
            Log.log(e);
            return;
        }

        final IAnalysisBuilderRunnable runnable = AnalysisBuilderRunnableFactory.createRunnable(document, resource,
                moduleCallback, isFullBuild(), moduleName, forceAnalysis, analysisCause, nature, documentTime,
                resource.getModificationStamp());

        if (runnable == null) {
            //It may be null if the document version of the new one is lower than one already active.
            return;
        }

        execRunnable(moduleName, runnable, forceAnalyzeInThisThread);
    }

    /**
     * Depending on whether we're in a full build or delta build, this method will run the runnable directly
     * or schedule it as a job.
     * @param forceAnalyzeInThisThread 
     */
    private void execRunnable(final String moduleName, final IAnalysisBuilderRunnable runnable,
            boolean forceAnalyzeInThisThread) {
        if (isFullBuild() || forceAnalyzeInThisThread) {
            runnable.run();
        } else {
            RunnableAsJobsPoolThread.getSingleton().scheduleToRun(runnable, "PyDev: Code Analysis:" + moduleName);
        }
    }

    @Override
    public void visitRemovedResource(IResource resource, ICallback0<IDocument> document, IProgressMonitor monitor) {
        PythonNature nature = getPythonNature(resource);
        if (nature == null) {
            return;
        }
        if (resource.getType() == IResource.FOLDER) {
            //We don't need to explicitly treat any folder (just its children -- such as __init__ and submodules)
            return;
        }
        if (!isFullBuild()) {
            //on a full build, it'll already remove all the info
            String moduleName;
            try {
                moduleName = getModuleName(resource, nature);
            } catch (MisconfigurationException e) {
                Log.log(e);
                return;
            }

            long documentTime = this.getDocumentTime();
            if (documentTime == -1) {
                Log.log("Warning: The document time in the visitor for remove is -1. Changing for current time. "
                        + "Resource: " + resource + ". Module name: " + moduleName);
                documentTime = System.currentTimeMillis();
            }
            long resourceModificationStamp = resource.getModificationStamp();

            final IAnalysisBuilderRunnable runnable = AnalysisBuilderRunnableFactory.createRunnable(moduleName, nature,
                    isFullBuild(), false, AnalysisBuilderRunnable.ANALYSIS_CAUSE_BUILDER, documentTime,
                    resourceModificationStamp);

            if (runnable == null) {
                //It may be null if the document version of the new one is lower than one already active.
                return;
            }

            execRunnable(moduleName, runnable, false);
        }
    }

    @Override
    public void visitingWillStart(IProgressMonitor monitor, boolean isFullBuild, IPythonNature nature) {
        if (isFullBuild) {
            AbstractAdditionalDependencyInfo info;
            try {
                info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
            } catch (MisconfigurationException e) {
                Log.log(e);
                return;
            }

            info.clearAllInfo();
        }
    }

}
