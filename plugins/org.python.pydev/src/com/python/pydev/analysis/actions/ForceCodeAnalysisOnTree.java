/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IObjectActionDelegate;
import org.python.pydev.ast.builder.PyDevBuilderVisitor;
import org.python.pydev.ast.builder.VisitorMemo;
import org.python.pydev.ast.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.ast.listing_utils.PyFileListing;
import org.python.pydev.core.BaseModuleRequest;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IModule;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.ui.actions.resources.PyResourceAction;

import com.python.pydev.analysis.additionalinfo.builders.AnalysisBuilderRunnable;
import com.python.pydev.analysis.additionalinfo.builders.AnalysisBuilderVisitor;
import com.python.pydev.analysis.additionalinfo.builders.AnalysisRunner;
import com.python.pydev.analysis.external.IExternalCodeAnalysisVisitor;
import com.python.pydev.analysis.flake8.Flake8VisitorFactory;
import com.python.pydev.analysis.mypy.MypyVisitorFactory;
import com.python.pydev.analysis.pylint.PyLintVisitorFactory;
import com.python.pydev.analysis.ruff.RuffVisitorFactory;

/**
 * @author fabioz
 *
 */
public class ForceCodeAnalysisOnTree extends PyResourceAction implements IObjectActionDelegate {

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#confirmRun()
     */
    @Override
    protected boolean confirmRun() {
        return true;
    }

    private Set<IFile> filesVisited = new HashSet<IFile>();

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#afterRun(int)
     */
    @Override
    protected void afterRun(int resourcesAffected) {
        filesVisited.clear();
    }

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#beforeRun()
     */
    @Override
    protected void beforeRun() {
        filesVisited.clear();
    }

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#doActionOnResource(org.eclipse.core.resources.IResource, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected int doActionOnResource(IResource next, IProgressMonitor monitor) {
        List<IExternalCodeAnalysisVisitor> externalVisitors = new ArrayList<IExternalCodeAnalysisVisitor>();
        List<IFile> filesToVisit = new ArrayList<IFile>();
        PythonNature nature = PythonNature.getPythonNature(next);
        if (nature == null) {
            return 1;
        }
        if (next instanceof IContainer) {
            List<IFile> l = PyFileListing.getAllIFilesBelow((IContainer) next);

            for (Iterator<IFile> iter = l.iterator(); iter.hasNext();) {
                IFile element = iter.next();
                if (element != null) {
                    if (PythonPathHelper.isValidSourceFile(element)) {
                        filesToVisit.add(element);
                    }
                }
            }
            IExternalCodeAnalysisVisitor pyLintVisitor = PyLintVisitorFactory.create(next, null, null, monitor);
            IExternalCodeAnalysisVisitor mypyVisitor = MypyVisitorFactory.create(next, null, null, monitor);
            IExternalCodeAnalysisVisitor flake8Visitor = Flake8VisitorFactory.create(next, null, null, monitor);
            IExternalCodeAnalysisVisitor ruffVisitor = RuffVisitorFactory.create(next, null, null, monitor);
            externalVisitors.add(pyLintVisitor);
            externalVisitors.add(mypyVisitor);
            externalVisitors.add(flake8Visitor);
            externalVisitors.add(ruffVisitor);
        } else if (next instanceof IFile) {
            if (PythonPathHelper.isValidSourceFile((IFile) next)) {
                filesToVisit.add((IFile) next);
            }
        }

        forceCodeAnalysisOnFiles(nature, monitor, filesToVisit, filesVisited, externalVisitors);

        return 1;
    }

    public static void forceCodeAnalysisOnFiles(PythonNature nature, IProgressMonitor monitor, List<IFile> filesToVisit,
            Set<IFile> filesVisited, List<IExternalCodeAnalysisVisitor> externalVisitors) {
        if (nature == null) {
            return;
        }
        AnalysisBuilderVisitor visitor = new AnalysisBuilderVisitor();
        visitor.visitingWillStart(new NullProgressMonitor(), false, null);
        FastStringBuffer buf = new FastStringBuffer();
        for (IFile f : filesToVisit) {
            if (monitor.isCanceled()) {
                break;
            }
            if (filesVisited.contains(f)) {
                continue;
            }
            filesVisited.add(f);

            monitor.setTaskName(buf.clear().append("Scheduling: ").append(f.getName()).toString());
            IDocument doc = FileUtilsFileBuffer.getDocFromResource(f);
            visitor.memo = new VisitorMemo();
            visitor.memo.put(PyDevBuilderVisitor.IS_FULL_BUILD, false);
            long documentTime = System.currentTimeMillis();
            visitor.memo.put(PyDevBuilderVisitor.DOCUMENT_TIME, documentTime);
            String moduleName;
            try {
                moduleName = nature.resolveModule(f);
            } catch (MisconfigurationException e) {
                Log.log(e);
                continue;
            }
            if (moduleName == null) {
                continue;
            }
            AnalysisBuilderVisitor.setModuleNameInCache(visitor.memo, f, moduleName);

            if (!PythonPathHelper.isValidSourceFile(f)) {
                AnalysisRunner.deleteMarkers(f, true);
                continue;
            }

            IModule module = nature.getAstManager().getModule(moduleName, nature, true, new BaseModuleRequest(false));
            if (module == null) {
                Log.log(IStatus.WARNING, "Unable to get module: " + moduleName + " for resource: " + f, null);
                continue;
            }
            visitor.doVisitChangedResource(nature, f, doc, null, module, new NullProgressMonitor(), true,
                    AnalysisBuilderRunnable.ANALYSIS_CAUSE_PARSER, documentTime, false, externalVisitors);
        }
        visitor.visitingEnded(new NullProgressMonitor());
    }

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#needsUIThread()
     */
    @Override
    protected boolean needsUIThread() {
        return false;
    }
}
